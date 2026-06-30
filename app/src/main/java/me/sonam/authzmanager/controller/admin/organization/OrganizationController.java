package me.sonam.authzmanager.controller.admin.organization;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import me.sonam.authzmanager.AuthzManagerException;
import me.sonam.authzmanager.clients.user.OrganizationChoice;
import me.sonam.authzmanager.controller.util.MessageConstants;
import me.sonam.authzmanager.controller.util.Util;
import me.sonam.authzmanager.rest.RestPage;
import me.sonam.authzmanager.service.UserSearchPolicyService;
import me.sonam.authzmanager.tenant.TenantAuthorizationUrlResolver;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.OrganizationWebClient;
import me.sonam.authzmanager.webclients.RoleWebClient;
import me.sonam.authzmanager.clients.user.User;

import me.sonam.authzmanager.webclients.UserWebClient;
import org.apache.tomcat.websocket.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.*;

@Controller
@RequestMapping("/admin/organizations")
public class OrganizationController {
    private static final Logger LOG = LoggerFactory.getLogger(OrganizationController.class);
    private static final String USER_IN_ANOTHER_ORG_MESSAGE = "user is already in another organization";

    private final OrganizationWebClient organizationWebClient;
    private final RoleWebClient roleWebClient;
    private final UserWebClient userWebClient;
    private final TokenService tokenService;
    private final UserSearchPolicyService userSearchPolicyService;
    private final TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver;

    public OrganizationController(OrganizationWebClient organizationWebClient, RoleWebClient roleWebClient,
                                  UserWebClient userWebClient, TokenService tokenService,
                                  UserSearchPolicyService userSearchPolicyService,
                                  TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver) {
        this.organizationWebClient = organizationWebClient;
        this.roleWebClient = roleWebClient;
        this.userWebClient = userWebClient;
        this.tokenService = tokenService;
        this.userSearchPolicyService = userSearchPolicyService;
        this.tenantAuthorizationUrlResolver = tenantAuthorizationUrlResolver;
    }

    /**
     * show the default organization
     *
     * @param model
     * @return
     */
    @GetMapping
    public Mono<String> getOrganizations(Model model, Pageable pageable1, HttpServletRequest request) {
        LOG.info("get organization");
        final String PATH = "/admin/organizations/list";
        int pageSize = 5;

        DefaultOidcUser oidcUser = (DefaultOidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userIdString = oidcUser.getAttribute("userId");
        LOG.info("oidc.userId: {}", userIdString);
        UUID userId = UUID.fromString(userIdString);

        final String accessToken = tokenService.getAccessToken();

        if (pageable1.getPageSize() < 100) {
            pageSize = pageable1.getPageSize();
            LOG.info("taking page size from pageable: {}", pageSize);
        }

        final Pageable pageable = PageRequest.of(pageable1.getPageNumber(), pageSize, Sort.by("name"));
        String organizationHost = tenantAuthorizationUrlResolver.currentAuthorizationHost();

        return  roleWebClient.getOrgAdminOrganizationIdsForUser(accessToken, pageable)
                .flatMap(uuidPage ->
                    organizationWebClient.getOrganizationByIdsIn(accessToken, uuidPage.content()).zipWith(Mono.just(uuidPage)))
                .flatMap(objects -> organizationWebClient.getDefaultOrganizationIdForUser(accessToken,
                        userId, organizationHost).zipWith(Mono.just(objects)))
                        .doOnNext(objects -> {
                    RestPage<UUID> uuidPage = objects.getT2().getT2();
                    List<Organization> list = objects.getT2().getT1();

                    list.forEach(organization -> {
                        if (organization.getId().equals(objects.getT1())) {
                            organization.setDefaultOrganization(true);
                            LOG.info("set organization as default");
                        }
                    });

                    RestPage<Organization> page = new RestPage<>(list, uuidPage.number(), uuidPage.size(), uuidPage.totalElements());

                    model.addAttribute("page", page);
                }).thenReturn(PATH);

    }

    @PostMapping
    public Mono<String> updateOrganization(@Valid @ModelAttribute("organization") Organization organization,
                                           BindingResult bindingResult, Model model) {
        final String PATH = "admin/organizations/form";
        HttpMethod httpMethod;

        if (organization.getId() == null) {
            LOG.info("no id, this is for create");
            model.addAttribute("message", "no support for creating organization");
            return Mono.just(PATH);
        }

        if (bindingResult.hasErrors()) {
            LOG.info("user didn't enter required fields");
            model.addAttribute("error", "Data validation failed");
            return Mono.just(PATH);
        }
        UUID userId = Util.getLoggedInUserId();

        Organization org = new Organization(organization.getId(), organization.getName(), userId);
        LOG.info("create organization from organization: {}", organization);

        final String accessToken = tokenService.getAccessToken();

        return roleWebClient.isOrgAdminInOrgId(accessToken, userId, organization.getId())
                .flatMap(isOrgAdmin -> {
                    if (!isOrgAdmin) {
                        model.addAttribute("error", MessageConstants.NOT_ORG_ADMIN + " " + organization.getId());
                        return Mono.error(new AuthenticationException(MessageConstants.NOT_ORG_ADMIN));
                    }
                    return organizationWebClient.updateOrganization(accessToken, org, HttpMethod.PUT);
                })
                .flatMap(organization1 -> {
                    LOG.info("got back response: {}", organization1);
                    model.addAttribute("organization", organization1);
                    model.addAttribute("message", "organization updated successfully");

                    if (organization.getDefaultOrganization() != null){

                        if(organization.getDefaultOrganization()) {
                            // user set the org as default
                            organization1.setPreviousDefaultOrganization(true);
                            organization1.setDefaultOrganization(true);
                            LOG.info("call organization service to set this org as default org");
                            return organizationWebClient.setDefaultOrganization(accessToken, org.getId(), userId)
                                    .thenReturn(PATH);
                        }
                        else if(!organization.getDefaultOrganization()) {
                            LOG.info("default checkbox is shown but not selected");
                            organization1.setPreviousDefaultOrganization(false);
                            organization1.setDefaultOrganization(false);
                        }
                    }
                    else {
                        if(organization.isPreviousDefaultOrganization()) {
                            LOG.info("checkbox is disabled and previously true");
                            organization1.setPreviousDefaultOrganization(true);
                            organization1.setDefaultOrganization(true);
                        }

                    }
                    return Mono.just(PATH);
                }).onErrorResume(throwable -> {
                    LOG.error("error occurred {}", throwable.getMessage());

                    return Mono.just(PATH);
                });
    }

    @GetMapping("/{id}")
    public Mono<String> getOrganizationById(@PathVariable("id") UUID id, Model model, HttpServletRequest request) {
        final String PATH = "admin/organizations/form";
        LOG.info("get organization by id: {}", id);

        final String accessToken = tokenService.getAccessToken();
        UUID userId = Util.getLoggedInUserId();
        String organizationHost = tenantAuthorizationUrlResolver.currentAuthorizationHost();

        return organizationWebClient.getOrganizationById(accessToken, id)
                .flatMap(organization -> requireOrgAdminOrSubdomainAdmin(accessToken, userId, organizationHost, organization, model))
                .flatMap(objects -> organizationWebClient.getDefaultOrganizationIdForUser(accessToken,
                        userId, organizationHost).zipWith(Mono.just(objects)))
                .flatMap(objects -> {
                    Organization organization = objects.getT2();

                    if (objects.getT1().equals(organization.getId())) {
                        LOG.info("the selected orgId is default organization: {}", organization.getId());
                        organization.setDefaultOrganization(true);
                        organization.setPreviousDefaultOrganization(true);
                    }
                    model.addAttribute("organization", organization);

                    return Mono.just(PATH);
                });
    }

    private Mono<Organization> requireOrgAdminOrSubdomainAdmin(String accessToken, UUID userId, String organizationHost,
                                                               Organization organization, Model model) {
        return roleWebClient.isOrgAdminInOrgId(accessToken, userId, organization.getId())
                .flatMap(isOrgAdmin -> {
                    if (isOrgAdmin) {
                        return Mono.just(organization);
                    }
                    return organizationWebClient.getSubdomainByHost(accessToken, organizationHost)
                            .flatMap(subdomain -> roleWebClient.isSubdomainAdminInSubdomainId(accessToken, userId,
                                            subdomain.getId()))
                            .flatMap(isSubdomainAdmin -> {
                                if (!isSubdomainAdmin) {
                                    model.addAttribute("error",
                                            "You are not an OrgAdmin for this organization or a SubdomainAdmin for this subdomain");
                                    return Mono.error(new AuthenticationException(
                                            "You are not an OrgAdmin for orgId or SubdomainAdmin for subdomain: "
                                                    + organization.getName()));
                                }
                                return organizationWebClient.organizationBelongsToSubdomain(accessToken,
                                                organization.getId(), organizationHost)
                                        .thenReturn(organization);
                            });
                });
    }


    @GetMapping("/{id}/roles")
    public Mono<String> getRolesForOrganizationId(@PathVariable("id") UUID id, Model model, Pageable userPageable,
                                                  HttpServletRequest request) {
        final String PATH = "admin/organizations/roles";
        LOG.info("get roles for organization by id: {}", id);

        int pageSize = 5;

        if (userPageable.getPageSize() < 100) {
            pageSize = userPageable.getPageSize();
            LOG.info("taking page size from pageable: {}", pageSize);
        }
        UUID userId = Util.getLoggedInUserId();

        Pageable pageable = PageRequest.of(userPageable.getPageNumber(), pageSize, Sort.by("name"));
        String accessToken = tokenService.getAccessToken();
        String organizationHost = tenantAuthorizationUrlResolver.currentAuthorizationHost();
        return organizationWebClient.getDefaultOrganizationIdForUser(accessToken, userId, organizationHost)
                .doOnNext(uuid -> {
                    LOG.info("add defaultOrganizationId to model: {}", uuid);
                    model.addAttribute("defaultOrganizationId", uuid);
                })
                .flatMap(orgId ->roleWebClient.isOrgAdminInOrgId(accessToken, userId, id))
                .flatMap(isOrgAdmin -> {
                    if (!isOrgAdmin) {
                        model.addAttribute("error", MessageConstants.NOT_ORG_ADMIN + " " + id);
                        return Mono.error(new AuthenticationException(MessageConstants.NOT_ORG_ADMIN));
                    }
                    return organizationWebClient.getOrganizationById(accessToken, id);
                })
                 .doOnNext(organization -> model.addAttribute("organization", organization))
                .flatMap(organization -> roleWebClient.getRolesByOrganizationId(accessToken, id, pageable))
                .doOnNext(roleRestPage -> model.addAttribute("page", roleRestPage))
                .thenReturn(PATH);
    }

    /*
    get users in the organization id
     */
    @GetMapping("/{id}/users")
    public Mono<String> getUserForOrganizationId(@PathVariable("id") UUID id, Model model, Pageable userPageable) {
        String accessToken = tokenService.getAccessToken();
        UUID userId = Util.getLoggedInUserId();

        return getUsersForOrganization(id, userId, accessToken, model, userPageable);
    }

    private Mono<String> getUsersForOrganization(UUID organizationId, UUID userId, String accessToken, Model model, Pageable userPageable) {
        final String PATH = "admin/organizations/user";
        LOG.info("get users for organization by id: {}", organizationId);
        int pageSize = 5;

        if (userPageable.getPageSize() < 100) {
            pageSize = userPageable.getPageSize();
            LOG.info("taking page size from pageable: {}", pageSize);
        }
        Pageable pageable = PageRequest.of(userPageable.getPageNumber(), pageSize);

        return roleWebClient.isOrgAdminInOrgId(accessToken, userId, organizationId)
                .flatMap(isOrgAdmin -> {
                    if (!isOrgAdmin) {
                        model.addAttribute("error", MessageConstants.NOT_ORG_ADMIN + " " + organizationId);
                        return Mono.error(new AuthenticationException(MessageConstants.NOT_ORG_ADMIN));
                    }
                    return organizationWebClient.getOrganizationById(accessToken, organizationId);
                })
                .doOnNext(organization -> model.addAttribute("organization", organization))
                .flatMap(organization -> organizationWebClient.getUserIdsInOrganizationId(accessToken, organization.getId(), pageable))
                .flatMap(uuidPage -> {
                    LOG.info("uuidPage: {}", uuidPage.content());
                    model.addAttribute("page", uuidPage);
                    return userWebClient.getUserByBatchOfIds(accessToken, uuidPage.content());
                })
                .doOnNext(users -> {
                    LOG.info("got users: {}", users);
                    List<User> removableUsers = new ArrayList<>();
                    List<User> nonRemovableUsers = new ArrayList<>();
                    for(User user: users) {
                        if (!user.getId().equals(userId)) {
                            removableUsers.add(user);
                        }
                        else {
                            nonRemovableUsers.add(user);
                        }
                    }
                    model.addAttribute("removableUsers", removableUsers);
                    model.addAttribute("nonRemovableUsers", nonRemovableUsers);

                })
                .thenReturn(PATH);
    }

    @PostMapping("/{id}/users")
    public Mono<String> findUserByAuthenticationId(@PathVariable("id") UUID organizationId,
                                                   @ModelAttribute("username") String authenticationId, final Model model, Pageable userPageable) {
        final String PATH = "admin/organizations/user";
        LOG.info("find user by authenticationId: {}", authenticationId);
        final String accessToken = tokenService.getAccessToken();
        UUID userId = Util.getLoggedInUserId();
        String organizationHost = tenantAuthorizationUrlResolver.currentAuthorizationHost();

        return roleWebClient.isOrgAdminInOrgId(accessToken, userId, organizationId)
                .flatMap(aBoolean -> {
                    if (!aBoolean)  {
                        model.addAttribute("error", MessageConstants.NOT_ORG_ADMIN + " " + organizationId);
                        return Mono.error(new AuthenticationException(MessageConstants.NOT_ORG_ADMIN));
                    }
                    return Mono.just(aBoolean);
                }).flatMap(aBoolean -> organizationWebClient.getOrganizationById(accessToken, organizationId))
                .doOnNext(organization -> model.addAttribute("organization", organization))
                .flatMap(organization -> {
                    int pageSize = 5;

                    if (userPageable.getPageSize() < 100) {
                        pageSize = userPageable.getPageSize();
                        LOG.info("taking page size from pageable: {}", pageSize);
                    }
                    Pageable pageable = PageRequest.of(userPageable.getPageNumber(), pageSize);
                    return organizationWebClient.getUserIdsInOrganizationId(accessToken, organization.getId(), pageable)
                            .flatMap(uuidPage -> {
                                LOG.info("uuidPage: {}", uuidPage.content());
                                model.addAttribute("page", uuidPage);
                                return userWebClient.getUserByBatchOfIds(accessToken, uuidPage.content());
                            })
                            .doOnNext(users -> {
                                LOG.info("got users: {}", users);
                                model.addAttribute("users", users);
                            }).thenReturn(organization);
                })
                .flatMap(organization -> findUserWithinSearchPolicy(accessToken, authenticationId, organizationHost))
                .doOnNext(user -> {
                    LOG.info("found user: {}", user);
                    model.addAttribute("message", "Found user with username '" + authenticationId + "'");
                    model.addAttribute("user", user);

                }).flatMap(user -> {
                    LOG.info("checking user.id {} exists in organization, user {}", user.getId(), user);
                    return organizationWebClient.userExistsInOrganization(accessToken, user.getId(), organizationId)
                            .flatMap(aBoolean -> {
                                LOG.info("user exists? : {}", aBoolean);
                                user.getOrganizationChoice().setOrganizationId(organizationId);
                                user.getOrganizationChoice().setSelected(aBoolean);
                                if (aBoolean) {
                                    return organizationWebClient.getDefaultOrganizationIdForUser(accessToken,
                                                    user.getId(), organizationHost)
                                            .map(organizationId::equals)
                                            .defaultIfEmpty(false)
                                            .doOnNext(user.getOrganizationChoice()::setDefaultOrganization)
                                            .doOnNext(defaultOrganization -> model.addAttribute("user", user));
                                }
                                user.getOrganizationChoice().setDefaultOrganization(false);
                                model.addAttribute("user", user);
                                return Mono.just(false);
                            });
                }
                )
                .onErrorResume(throwable -> {
                    LOG.error("failed to find user: {}", throwable.getMessage());

                    if (USER_IN_ANOTHER_ORG_MESSAGE.equals(throwable.getMessage())) {
                        model.addAttribute("message", USER_IN_ANOTHER_ORG_MESSAGE);
                    }
                    else {
                        model.addAttribute("message", "failed to find user, " + throwable.getMessage());
                    }
                    return Mono.just(false);
                })
                .thenReturn(PATH);

    }

    private Mono<User> findUserWithinSearchPolicy(String accessToken, String authenticationId, String organizationHost) {
        return userSearchPolicyService.validateSearch(authenticationId, organizationHost)
                .<Mono<User>>map(error -> Mono.error(new AuthzManagerException(error)))
                .orElseGet(() -> userWebClient.findByAuthenticationProfileSearch(accessToken, authenticationId));
    }


    /**
     * this method will handle the form's POST method to associate user to organization:
     * Add action: add the user to organization.
     * Remove action: remove the user from organization.
     * @param user
     * @param model
     * @param pageable
     * @return
     */

    @PostMapping("/{id}/users/add")
    public Mono<String> updateUserOrganization(@PathVariable("id") UUID orgId, @ModelAttribute("user") User user,
                                               @RequestParam("action") String action,
                                               Model model, Pageable pageable) {
        final String PATH = "admin/organizations/user";
        LOG.info("update user in organization with action: {}", action);

        final String accessToken = tokenService.getAccessToken();
        UUID userId = Util.getLoggedInUserId();
        String organizationHost = tenantAuthorizationUrlResolver.currentAuthorizationHost();
        LOG.info("organizationHost: {}", organizationHost);

        return organizationWebClient.getOrganizationById(accessToken, orgId)
                .flatMap(organization -> roleWebClient.isOrgAdminInOrgId(accessToken, userId, organization.getId()).zipWith(Mono.just(organization)))
                .flatMap(objects -> {
                    if (!objects.getT1()) {
                        model.addAttribute("error", MessageConstants.NOT_ORG_ADMIN + " " + orgId);
                        return Mono.error(new AuthenticationException(MessageConstants.NOT_ORG_ADMIN));
                    }

                    if ("add".equals(action)) {
                        LOG.info("add user to organization action selected");

                        return addUserToOrganization(PATH, user, userId, objects.getT2(), accessToken,
                                model, pageable, organizationHost);
                    }
                    else if ("remove".equals(action)) {
                        LOG.info("remove user from organization action selected");
                        return removeUserFromOrganization(PATH, user, userId, objects.getT2(), accessToken, model, pageable);

                    }
                    else if ("default".equals(action)) {
                        LOG.info("set default organization action selected");
                        model.addAttribute("organization", objects.getT2());
                        return organizationWebClient.setDefaultOrganization(accessToken,
                                        user.getOrganizationChoice().getOrganizationId(), user.getId())
                                .doOnNext(message -> {
                                    user.getOrganizationChoice().setSelected(true);
                                    user.getOrganizationChoice().setDefaultOrganization(true);
                                    model.addAttribute("user", user);
                                    model.addAttribute("message",
                                            "default organization updated for username: " + user.getAuthenticationId());
                                })
                                .then(getUsersInOrganization(PATH, userId, objects.getT2(), accessToken, model, pageable));
                    }

                    model.addAttribute("message", "invalid user organization action: " + action);
                    return Mono.just(PATH);
                }).thenReturn(PATH);
    }

    @DeleteMapping("/{id}/users/{userId}/authenticationId/{authenticationId}")
    public Mono<String> deleteUserFromOrganization(@PathVariable("id") UUID orgId, @PathVariable("userId") UUID userId,
                                                   @PathVariable("authenticationId") String authenticationId,
                                                   Model model, Pageable pageable) {
        LOG.info("delete user-id {} from organization {}", userId, orgId);
        final String PATH = "admin/organizations/user";
        User user = new User(userId);
        user.setAuthenticationId(authenticationId);
        user.setOrganizationChoice(new OrganizationChoice(orgId));
        final String accessToken = tokenService.getAccessToken();
        UUID loggedUserId = Util.getLoggedInUserId();

        return organizationWebClient.getOrganizationById(accessToken, orgId)
                .flatMap(organization -> removeUserFromOrganization(PATH, user, loggedUserId, organization, accessToken, model, pageable));
    }

    private Mono<String> addUserToOrganization(final String PATH, User user, UUID loggedInUserId, Organization organization,
                                               String accessToken, Model model, Pageable userPageable, String subdomain) {
        LOG.info("add user to organization: {}", user);

        model.addAttribute("organization", organization);

        return organizationWebClient.addUserToOrganization(accessToken, user.getId(),
                        user.getOrganizationChoice().getOrganizationId(), subdomain, true)
                .flatMap(stringStringMap -> setDefaultOrganizationIfRequested(accessToken, user)
                        .thenReturn(stringStringMap))
                .doOnNext(stringStringMap -> {
                    model.addAttribute("message", "user successfully added to organization with username: "+ user.getAuthenticationId());
                    LOG.info("added to user to organization, nullify the user so the form does not show this user again");
                    model.addAttribute("user", null);
                })
                .flatMap(stringStringMap -> {
                    int pageSize = 5;

                    if (userPageable.getPageSize() < 100) {
                        pageSize = userPageable.getPageSize();
                        LOG.info("taking page size from pageable: {}", pageSize);
                    }
                    Pageable pageable = PageRequest.of(userPageable.getPageNumber(), pageSize);
                    return getUsersInOrganization(PATH, loggedInUserId, organization, accessToken, model, pageable);
                }).onErrorResume(throwable -> {
                    LOG.error("error occured during adding user to organization", throwable);
                    model.addAttribute("message", "error occured during adding user to organization: " + throwable.getMessage());
                  return  Mono.just(PATH);
                });
    }

    private Mono<String> setDefaultOrganizationIfRequested(String accessToken, User user) {
        if (!Boolean.TRUE.equals(user.getOrganizationChoice().getDefaultOrganization())) {
            return Mono.empty();
        }

        LOG.info("set organization {} as default for user {}",
                user.getOrganizationChoice().getOrganizationId(), user.getId());
        return organizationWebClient.setDefaultOrganization(accessToken,
                user.getOrganizationChoice().getOrganizationId(), user.getId());
    }

    private Mono<String> removeUserFromOrganization(final String PATH, User user, UUID loggedInUserId, Organization organization, String accessToken, Model model, Pageable userPageable) {
        LOG.info("remove user from organization: {}", user);

        model.addAttribute("organization", organization);
        model.addAttribute("user", null);
        LOG.info("removed user from organization, null the user so the form does not show this user again");

        return organizationWebClient.removeUserFromOrganization(accessToken, user.getId(), user.getOrganizationChoice().getOrganizationId())
                .doOnNext(stringStringMap -> model.addAttribute("message", "user removed from organization successfully with username: "+user.getAuthenticationId()))
                .flatMap(stringStringMap -> {
                    int pageSize = 5;

                    if (userPageable.getPageSize() < 100) {
                        pageSize = userPageable.getPageSize();
                        LOG.info("taking page size from pageable: {}", pageSize);
                    }
                    Pageable pageable = PageRequest.of(userPageable.getPageNumber(), pageSize);
                    return getUsersInOrganization(PATH, loggedInUserId, organization, accessToken, model, pageable);
                }).onErrorResume(throwable -> {
                    LOG.error("error occurred when removing user from organization", throwable);
                    model.addAttribute("message", "error occurred when removing user from organization: " + throwable.getMessage());
                    return Mono.just(PATH);
                });
    }

    @PostMapping(path = "/{id}/default")
    public Mono<String> setUserDefaultOrganization(@PathVariable("id") UUID id,
                                                Model model, final Pageable userPageable) {
        LOG.info("set organization.id {} as default organization for user", id);

        final String path = "/admin/organizations";
        Pageable pageable = PageRequest.of(userPageable.getPageNumber(), 5, Sort.by("name"));

        return Mono.just(path);
    }

    private Mono<String> getUsersInOrganization(final String PATH, UUID loggedInUserId, Organization organization, String accessToken, Model model, Pageable pageable) {
         return organizationWebClient.getUserIdsInOrganizationId(accessToken, organization.getId(), pageable)
                .flatMap(uuidPage -> {
                    LOG.info("uuidPage: {}", uuidPage.content());
                    model.addAttribute("page", uuidPage);
                    return userWebClient.getUserByBatchOfIds(accessToken, uuidPage.content());
                })
                .doOnNext(users -> {
                    LOG.info("got users: {}", users);
                    List<User> removableUsers = new ArrayList<>();
                    List<User> nonRemovableUsers = new ArrayList<>();
                    for(User user: users) {
                        if (!user.getId().equals(loggedInUserId)) {
                            removableUsers.add(user);
                        }
                        else {
                            nonRemovableUsers.add(user);
                        }
                    }
                    model.addAttribute("removableUsers", removableUsers);
                    model.addAttribute("nonRemovableUsers", nonRemovableUsers);

                })
                .thenReturn(PATH);
    }
}
