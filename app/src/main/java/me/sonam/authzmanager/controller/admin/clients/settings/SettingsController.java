package me.sonam.authzmanager.controller.admin.clients.settings;

import jakarta.servlet.http.HttpServletRequest;
import me.sonam.authzmanager.AuthzManagerException;
import me.sonam.authzmanager.clients.user.User;
import me.sonam.authzmanager.controller.util.Util;
import me.sonam.authzmanager.rest.RestPage;
import me.sonam.authzmanager.service.UserSearchPolicyService;
import me.sonam.authzmanager.tenant.TenantAuthorizationUrlResolver;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.OrganizationWebClient;
import me.sonam.authzmanager.webclients.RoleWebClient;
import me.sonam.authzmanager.webclients.UserWebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.*;

@Controller
@RequestMapping("/admin/settings")
public class SettingsController {
    private static final Logger LOG = LoggerFactory.getLogger(SettingsController.class);

    private final String settingsPage = "/admin/setting/account";
    private final OrganizationWebClient organizationWebClient;
    private final RoleWebClient roleWebClient;
    private final UserWebClient userWebClient;
    private final TokenService tokenService;
    private final UserSearchPolicyService userSearchPolicyService;
    private final TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver;

    public SettingsController(OrganizationWebClient organizationWebClient, RoleWebClient roleWebClient,
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

    /*
    get users in the organization id
     */
    @GetMapping
    public Mono<String> getUsersForDefaultOrganization(Model model, Pageable userPageable, HttpServletRequest request) {
        String accessToken = tokenService.getAccessToken();
        UUID userId = Util.getLoggedInUserId();

        LOG.info("get default organization users");

        int pageSize = 5;

        if (userPageable.getPageSize() < 100) {
            pageSize = userPageable.getPageSize();
            LOG.info("taking page size from pageable: {}", pageSize);
        }
        Pageable pageable = PageRequest.of(userPageable.getPageNumber(), pageSize);
        String organizationHost = tenantAuthorizationUrlResolver.currentAuthorizationHost();

        final String noDefaultOrgFound = "No Default organization found";

        return  roleWebClient.getAuthzManagerRoleByName(accessToken, "SuperAdmin")
                .doOnNext(stringStringMap -> {
                    LOG.info("superAdmin role map: {}", stringStringMap);
                    UUID uuid = UUID.fromString(stringStringMap.get("message"));
                    LOG.info("superAdmin id: {}", uuid);
                    model.addAttribute("authzManagerRoleId", uuid);
                })
                .switchIfEmpty(Mono.error(new AuthzManagerException("No SuperAdmin authzManagerRole found")))
                .flatMap(stringStringMap -> organizationWebClient.getDefaultOrganizationIdForUser(accessToken,
                        userId, organizationHost))
                .switchIfEmpty(Mono.error(new AuthzManagerException(noDefaultOrgFound)))
                .flatMap(orgId -> organizationWebClient.getOrganizationById(accessToken, orgId))
                .doOnNext(organization -> model.addAttribute("organizationId", organization.getId()))
                .doOnNext(organization -> model.addAttribute("organization", organization))
                .flatMap(organization -> organizationWebClient.getUserIdsInOrganizationId(accessToken, organization.getId(), pageable)
                        .switchIfEmpty(Mono.just(new RestPage<>(List.of(), pageable.getPageNumber(),pageable.getPageSize(), 0)))
                        .zipWith(Mono.just(organization)))

                        .flatMap(orgWithUserIdPage -> {
                            if (orgWithUserIdPage.getT1().isEmpty()) {
                                LOG.info("there are no userIds found for this page {}", pageable.getPageNumber());
                                LOG.info("add empty users list");
                                model.addAttribute("users", List.of());

                                LOG.info("add userIdPage to model");
                                model.addAttribute("page", orgWithUserIdPage.getT1());

                                return Mono.just(new ArrayList<User>()).zipWith(Mono.just(orgWithUserIdPage.getT2()).zipWith(Mono.just(orgWithUserIdPage.getT1())));
                            }
                            else {
                                return userWebClient.getUserByBatchOfIds(accessToken, orgWithUserIdPage.getT1().content())
                                        .doOnNext(users -> {

                                            LOG.info("add users to model: {}", users);
                                            model.addAttribute("users", users);

                                            LOG.info("add userIdPage to model");
                                            model.addAttribute("page", orgWithUserIdPage.getT1());
                                        }).zipWith(Mono.just(orgWithUserIdPage.getT2()).zipWith(Mono.just(orgWithUserIdPage.getT1())));
                            }
                        })
                        .flatMap(usersWithOrgAndUserIdPage ->
                             roleWebClient.areUsersSuperAdminInDefaultOrgId(accessToken,
                                    usersWithOrgAndUserIdPage.getT2().getT1().getId(),
                                    usersWithOrgAndUserIdPage.getT2().getT2().content()).zipWith(Mono.just(usersWithOrgAndUserIdPage.getT1()))
                        )
                .doOnNext(uuidBooleanMapWithUserList -> {
                    LOG.info("got uuidBooleanMap {}", uuidBooleanMapWithUserList);
                    List<User> userList = uuidBooleanMapWithUserList.getT2();

                    for(User user: userList) {
                        if (user.getId().equals(userId)) {
                            user.setEnabled(false);
                        }
                       UUID authzManagerRoleOrganizationId = uuidBooleanMapWithUserList.getT1().get(user.getId());
                        if (authzManagerRoleOrganizationId != null) {
                            user.setAuthzManagerRoleOrganizationId(authzManagerRoleOrganizationId);
                        }
                    }
                    //sort by showing authzManagerRoleOrganizationId not null on top of list
                    Collections.sort(userList, Comparator.comparing(User::getAuthzManagerRoleOrganizationId, Comparator.nullsLast(Comparator.naturalOrder())));
                })
                .thenReturn(settingsPage)
                .onErrorResume(throwable -> {
                    LOG.debug("Exception occurred", throwable);

                    if (throwable.getMessage().equals(noDefaultOrgFound)) {
                        model.addAttribute("error", "You need to set a default organization to enable user for SuperAdmin role");
                        LOG.info("add error message when default org not found");
                    }
                    else {
                        if (throwable instanceof WebClientResponseException) {
                            WebClientResponseException webClientResponseException = (WebClientResponseException) throwable;
                            String errorMessage = webClientResponseException.getResponseBodyAsString();
                            LOG.error("error body contains: {}", errorMessage);

                            model.addAttribute("error", errorMessage);
                        }
                        else {
                            LOG.error("exception caught with message: {}", throwable.getMessage());
                            model.addAttribute("error", "failed to get default organization users");
                        }

                    }

                    return Mono.just(settingsPage);
                });
    }

    @PostMapping
    public Mono<String> setUserSuperAdmin(@RequestParam("authzManagerRoleId")UUID authzManagerRoleId, @RequestParam("userId") UUID targetUserId,
                                          @RequestParam("organizationId")UUID organizationId, Model model,
                                          Pageable userPageable, HttpServletRequest request) {
        String accessToken = tokenService.getAccessToken();
        UUID loggedInUserId = Util.getLoggedInUserId();

        return roleWebClient.addUserToSuperAdminRoleInOrganization(accessToken, authzManagerRoleId, organizationId, targetUserId, userPageable)
                .doOnNext(stringObjectMap -> {
                    LOG.info("assigned user to superadmin role with a authzManagerRoleOrganiation id {}", stringObjectMap.get("id"));
                })
                .then(getUsersForDefaultOrganization(model, userPageable, request));
    }

    @DeleteMapping
    public Mono<String> deleteUserSuperAdmin(@RequestParam("authzManagerRoleOrganizationId")UUID authzManagerRoleOrganizationId) {
        LOG.info("delete user from SuperAdmin role by id {}", authzManagerRoleOrganizationId);

        String accessToken = tokenService.getAccessToken();
        UUID loggedInUserId = Util.getLoggedInUserId();

        return roleWebClient.deleteUserFromAuthzManagerRoleOrganization(accessToken, authzManagerRoleOrganizationId)
                .thenReturn("index");  //return index page that does not have any thymeleaf expressions
    }

    @PostMapping("{organizationId}/users")
    public Mono<String> findUserByAuthenticationId(@PathVariable("organizationId") UUID organizationId,
                                                   @ModelAttribute("username") String authenticationId, final Model model,
                                                   Pageable userPageable, HttpServletRequest request) {
        LOG.info("find user by authenticationId: {}", authenticationId);
        final String accessToken = tokenService.getAccessToken();
        UUID loggedInUserId = Util.getLoggedInUserId();

        return organizationWebClient.getOrganizationById(accessToken, organizationId)
                .doOnNext(organization -> model.addAttribute("organization", organization))
                .flatMap(organization -> findUserWithinSearchPolicy(accessToken, authenticationId))
                .doOnNext(user -> {
                    LOG.info("found user: {}", user);
                    model.addAttribute("message", "Found user with username '" + authenticationId + "'");
                    model.addAttribute("user", user);

                }).
                flatMap(user -> {
                            LOG.info("checking user.id {} exists in organization, user {}", user.getId(), user);
                            return organizationWebClient.userExistsInOrganization(accessToken, user.getId(), organizationId).zipWith(Mono.just(user));
                        }
                )
                .flatMap(objects -> {
                    if (!objects.getT1()) {
                        LOG.error("user does not exist in organization");
                        return Mono.error(new AuthzManagerException("user does not exist in this organization"));
                    }
                    else {
                        return Mono.just(objects);
                    }
                })
                .flatMap(objects -> showUserForDefaultOrganization(accessToken, loggedInUserId, objects.getT2(),
                        model, userPageable, tenantAuthorizationUrlResolver.currentAuthorizationHost()))
                .onErrorResume(throwable -> {
                    LOG.error("failed to find user: {}", throwable.getMessage());

                    model.addAttribute("message", "failed to find user, "+ throwable.getMessage());
                    return Mono.just(settingsPage);
                })
                .thenReturn(settingsPage);

    }

    private Mono<User> findUserWithinSearchPolicy(String accessToken, String authenticationId) {
        return userSearchPolicyService.validateSearch(authenticationId)
                .<Mono<User>>map(error -> Mono.error(new AuthzManagerException(error)))
                .orElseGet(() -> userWebClient.findByAuthenticationProfileSearch(accessToken, authenticationId));
    }

    // This is called to show this only `user` in the CustomRestPage when a user is found by searching for their username
    public Mono<String> showUserForDefaultOrganization(String accessToken, UUID loggedInUserId, User user,
                                                       Model model, Pageable userPageable, String subdomain) {
        LOG.info("get users for organization by id");

        int pageSize = 5;

        if (userPageable.getPageSize() < 100) {
            pageSize = userPageable.getPageSize();
            LOG.info("taking page size from pageable: {}", pageSize);
        }
        Pageable pageable = PageRequest.of(userPageable.getPageNumber(), pageSize);

        final String noDefaultOrgFound = "No Default organization found";

        return  roleWebClient.getAuthzManagerRoleByName(accessToken, "SuperAdmin")
                .doOnNext(stringStringMap -> {
                    LOG.info("superAdmin role map: {}", stringStringMap);
                    UUID uuid = UUID.fromString(stringStringMap.get("message"));
                    LOG.info("superAdmin id: {}", uuid);
                    model.addAttribute("authzManagerRoleId", uuid);
                })
                .switchIfEmpty(Mono.error(new AuthzManagerException("No SuperAdmin authzManagerRole found")))
                .flatMap(stringStringMap -> organizationWebClient.getDefaultOrganizationIdForUser(accessToken,
                        loggedInUserId, subdomain))
                .switchIfEmpty(Mono.error(new AuthzManagerException(noDefaultOrgFound)))
                .flatMap(orgId -> organizationWebClient.getOrganizationById(accessToken, orgId))
                .doOnNext(organization -> model.addAttribute("organizationId", organization.getId()))
                .doOnNext(organization -> model.addAttribute("organization", organization))
                .flatMap(organization -> {
                    if (user == null) {
                        return Mono.just(new RestPage<UUID>(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0))
                                .zipWith(Mono.just(organization));
                    }
                    else {
                        return Mono.just(new RestPage<UUID>(List.of(user.getId()), pageable.getPageNumber(), pageable.getPageSize(), 1))
                                .zipWith(Mono.just(organization));
                    }
                })
                .flatMap(orgWithUserIdPage -> {
                    if (orgWithUserIdPage.getT1().isEmpty()) {
                        LOG.info("there are no userIds found for this page {}", pageable.getPageNumber());
                        LOG.info("add empty users list");
                        model.addAttribute("users", List.of());

                        LOG.info("add userIdPage to model");
                        model.addAttribute("page", orgWithUserIdPage.getT1());

                        return Mono.just(new ArrayList<User>()).zipWith(Mono.just(orgWithUserIdPage.getT2()).zipWith(Mono.just(orgWithUserIdPage.getT1())));
                    }
                    else {
                        return Mono.just(List.of(user))
                                .doOnNext(users -> {

                                    LOG.info("add users to model: {}", users);
                                    model.addAttribute("users", users);

                                    LOG.info("add userIdPage to model");
                                    model.addAttribute("page", orgWithUserIdPage.getT1());
                                }).zipWith(Mono.just(orgWithUserIdPage.getT2()).zipWith(Mono.just(orgWithUserIdPage.getT1())));
                    }
                })
                .flatMap(usersWithOrgAndUserIdPage ->
                        roleWebClient.areUsersSuperAdminInDefaultOrgId(accessToken,
                                usersWithOrgAndUserIdPage.getT2().getT1().getId(),
                                usersWithOrgAndUserIdPage.getT2().getT2().content()).zipWith(Mono.just(usersWithOrgAndUserIdPage.getT1()))
                )
                .doOnNext(uuidBooleanMapWithUserList -> {
                    LOG.info("got uuidBooleanMap {}", uuidBooleanMapWithUserList);
                    List<User> userList = uuidBooleanMapWithUserList.getT2();

                    for(User userInList: userList) {
                        UUID authzManagerRoleOrganizationId = uuidBooleanMapWithUserList.getT1().get(userInList.getId());
                        if (authzManagerRoleOrganizationId != null) {
                            userInList.setAuthzManagerRoleOrganizationId(authzManagerRoleOrganizationId);
                        }
                    }
                })
                .thenReturn(settingsPage)
                .onErrorResume(throwable -> {
                    LOG.error("Exception occurred", throwable);

                    if (throwable.getMessage().equals(noDefaultOrgFound)) {
                        model.addAttribute("error", "You need to set a default organization to add user.");
                        LOG.info("add error message when default org not found");
                    }
                    else {
                        LOG.info("exception caught with message: {}", throwable.getMessage());
                    }

                    return Mono.just(settingsPage);
                });
    }
}
