package me.sonam.authzmanager.controller.admin.organization;

import jakarta.validation.Valid;
import me.sonam.authzmanager.clients.user.OrganizationChoice;
import me.sonam.authzmanager.controller.util.MessageConstants;
import me.sonam.authzmanager.controller.util.Util;
import me.sonam.authzmanager.rest.RestPage;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.OrganizationWebClient;
import me.sonam.authzmanager.webclients.RoleWebClient;
import me.sonam.authzmanager.clients.user.User;

import me.sonam.authzmanager.webclients.SettingWebClient;
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

    private final OrganizationWebClient organizationWebClient;
    private final RoleWebClient roleWebClient;
    private final UserWebClient userWebClient;
    private final TokenService tokenService;
    private final SettingWebClient settingWebClient;

    public OrganizationController(OrganizationWebClient organizationWebClient, RoleWebClient roleWebClient,
                                  UserWebClient userWebClient, SettingWebClient settingWebClient, TokenService tokenService) {
        this.organizationWebClient = organizationWebClient;
        this.roleWebClient = roleWebClient;
        this.userWebClient = userWebClient;
        this.settingWebClient = settingWebClient;
        this.tokenService = tokenService;
    }

    /**
     * show the default organization
     *
     * @param model
     * @return
     */
    @GetMapping
    public Mono<String> getOrganizations(Model model, Pageable pageable1) {
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

        return  roleWebClient.getOrgIdsOfSuperAdminOrganizationForUser(accessToken, pageable)
                .flatMap(uuidPage ->
                    organizationWebClient.getOrganizationByIdsIn(accessToken, uuidPage.content()).zipWith(Mono.just(uuidPage)))
                .flatMap(objects -> settingWebClient.getDefaultOrganization(accessToken, userId).zipWith(Mono.just(objects)))
                        .doOnNext(objects -> {
                    RestPage<UUID> uuidPage = objects.getT2().getT2();
                    List<Organization> list = objects.getT2().getT1();

                    list.forEach(organization -> {
                        if (organization.getId().equals(objects.getT1())) {
                            organization.setDefaultOrganization(true);
                            LOG.info("set organization as default");
                        }
                    });

                    RestPage<Organization> page = new RestPage<>(list, uuidPage.number(),
                                    uuidPage.size(), uuidPage.totalElements(), uuidPage.numberOfElements());

                    model.addAttribute("page", page);
                }).thenReturn(PATH);

    }

    @PostMapping
    public Mono<String> updateOrganization(@Valid @ModelAttribute("organization") Organization organization, BindingResult bindingResult, Model model) {
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

        return roleWebClient.isSuperAdminInOrgId(accessToken, userId, organization.getId())
                .flatMap(isSuperAdmin -> {
                    if (!isSuperAdmin) {
                        model.addAttribute("error", MessageConstants.NOT_SUPERADMIN + " " + organization.getId());
                        return Mono.error(new AuthenticationException(MessageConstants.NOT_SUPERADMIN));
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
                            LOG.info("call setting service to set this org as default org");
                            return settingWebClient.addDefaultOrganization(accessToken, userId, org.getId()).thenReturn(PATH);
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
    public Mono<String> getOrganizationById(@PathVariable("id") UUID id, Model model) {
        final String PATH = "admin/organizations/form";
        LOG.info("get organization by id: {}", id);

        final String accessToken = tokenService.getAccessToken();
        UUID userId = Util.getLoggedInUserId();

        return organizationWebClient.getOrganizationById(accessToken, id)
                .flatMap(organization -> roleWebClient.isSuperAdminInOrgId(accessToken, userId, organization.getId()).zipWith(Mono.just(organization)))
                .flatMap(objects -> {
                    if (!objects.getT1()) { //not supeadmin for orgId
                        model.addAttribute("error", "You are not a superadmin for this orgId "+objects.getT2().getName());
                        return Mono.error(new AuthenticationException("You are not a superadmin for orgId: "+objects.getT2().getName()));
                    }
                    return Mono.just(objects);
                })
                .flatMap(objects -> settingWebClient.getDefaultOrganization(accessToken, userId).zipWith(Mono.just(objects)))
                .flatMap(objects -> {
                    Organization organization = objects.getT2().getT2();

                    if (objects.getT1().equals(organization.getId())) {
                        LOG.info("the selected orgId is default organization: {}", organization.getId());
                        organization.setDefaultOrganization(true);
                        organization.setPreviousDefaultOrganization(true);
                    }
                    model.addAttribute("organization", organization);

                    return Mono.just(PATH);
                });
    }


    @GetMapping("/{id}/roles")
    public Mono<String> getRolesForOrganizationId(@PathVariable("id") UUID id, Model model, Pageable userPageable) {
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
        return settingWebClient.getDefaultOrganization(accessToken, userId)
                .doOnNext(uuid -> {
                    LOG.info("add defaultOrganizationId to model: {}", uuid);
                    model.addAttribute("defaultOrganizationId", uuid);
                })
                .flatMap(orgId ->roleWebClient.isSuperAdminInOrgId(accessToken, userId, id))
                .flatMap(isSuperAdmin -> {
                    if (!isSuperAdmin) {
                        model.addAttribute("error", MessageConstants.NOT_SUPERADMIN + " " + id);
                        return Mono.error(new AuthenticationException(MessageConstants.NOT_SUPERADMIN));
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

        return roleWebClient.isSuperAdminInOrgId(accessToken, userId, organizationId)
                .flatMap(isSuperAdmin -> {
                    if (!isSuperAdmin) {
                        model.addAttribute("error", MessageConstants.NOT_SUPERADMIN + " " + organizationId);
                        return Mono.error(new AuthenticationException(MessageConstants.NOT_SUPERADMIN));
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

        return roleWebClient.isSuperAdminInOrgId(accessToken, userId, organizationId)
                .flatMap(aBoolean -> {
                    if (!aBoolean)  {
                        model.addAttribute("error", MessageConstants.NOT_SUPERADMIN + " " + organizationId);
                        return Mono.error(new AuthenticationException(MessageConstants.NOT_SUPERADMIN));
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
                .flatMap(organization -> userWebClient.findByAuthenticationProfileSearch(accessToken, authenticationId))
                .doOnNext(user -> {
                    LOG.info("found user: {}", user);
                    model.addAttribute("message", "Found user with username '" + authenticationId + "'");
                    model.addAttribute("user", user);

                }).flatMap(user -> {
                    LOG.info("checking user.id {} exists in organization, user {}", user.getId(), user);
                    return organizationWebClient.userExistsInOrganization(accessToken, user.getId(), organizationId)
                            .doOnNext(aBoolean -> {
                                LOG.info("user exists? : {}", aBoolean);
                                user.getOrganizationChoice().setOrganizationId(organizationId);
                                user.getOrganizationChoice().setSelected(aBoolean);
                                //update the user in model
                                model.addAttribute("user", user);
                            });
                }
                )
                .doOnNext(aBoolean -> LOG.info("looks like it executed"))
                .onErrorResume(throwable -> {
                    LOG.error("failed to find user: {}", throwable.getMessage());

                    model.addAttribute("message", "failed to find user, "+ throwable.getMessage());
                    return Mono.just(false);
                })
                .thenReturn(PATH);

    }


    /**
     * this method will handle the form's POST method to associate user to organization:
     * Checked box: add the user to organization
     * Unchecked box: remove the user from organization
     * @param user
     * @param model
     * @param pageable
     * @return
     */

    @PostMapping("/{id}/users/add")  //remove as this is not used anymore
    public Mono<String> updateUserOrganization(@PathVariable("id") UUID orgId, @ModelAttribute("user") User user, Model model, Pageable pageable) {
        final String PATH = "admin/organizations/user";
        LOG.info("update user in organization");

        final String accessToken = tokenService.getAccessToken();
        UUID userId = Util.getLoggedInUserId();

        return organizationWebClient.getOrganizationById(accessToken, orgId)
                .flatMap(organization -> roleWebClient.isSuperAdminInOrgId(accessToken, userId, organization.getId()).zipWith(Mono.just(organization)))
                .flatMap(objects -> {
                    if (!objects.getT1()) {
                        model.addAttribute("error", MessageConstants.NOT_SUPERADMIN + " " + orgId);
                        return Mono.error(new AuthenticationException(MessageConstants.NOT_SUPERADMIN));
                    }

                    if (user.getOrganizationChoice().getSelected()) {
                        LOG.info("choice is selected to add user to organization");

                        return addUserToOrganization(PATH, user, userId, objects.getT2(), accessToken, model, pageable);
                    } else {
                        LOG.info("remove user from organization");
                        return removeUserFromOrganization(PATH, user, userId, objects.getT2(), accessToken, model, pageable);

                    }
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

    private Mono<String> addUserToOrganization(final String PATH, User user, UUID loggedInUserId, Organization organization, String accessToken, Model model, Pageable userPageable) {
        LOG.info("add user to organization: {}", user);

        model.addAttribute("organization", organization);

        return organizationWebClient.addUserToOrganization(accessToken, user.getId(), user.getOrganizationChoice().getOrganizationId())
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
