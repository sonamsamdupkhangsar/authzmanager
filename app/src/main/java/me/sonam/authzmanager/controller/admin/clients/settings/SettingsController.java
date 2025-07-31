package me.sonam.authzmanager.controller.admin.clients.settings;

import me.sonam.authzmanager.AuthzManagerException;
import me.sonam.authzmanager.clients.user.User;
import me.sonam.authzmanager.controller.UserSignupController;
import me.sonam.authzmanager.controller.admin.organization.OrganizationController;
import me.sonam.authzmanager.controller.util.Util;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.OrganizationWebClient;
import me.sonam.authzmanager.webclients.RoleWebClient;
import me.sonam.authzmanager.webclients.SettingWebClient;
import me.sonam.authzmanager.webclients.UserWebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/settings")
public class SettingsController {
    private static final Logger LOG = LoggerFactory.getLogger(SettingsController.class);

    private final String settingsPage = "/admin/setting/account";
    private final OrganizationWebClient organizationWebClient;
    private final RoleWebClient roleWebClient;
    private final UserWebClient userWebClient;
    private final TokenService tokenService;
    private final SettingWebClient settingWebClient;

    public SettingsController(OrganizationWebClient organizationWebClient, RoleWebClient roleWebClient,
                                  UserWebClient userWebClient, SettingWebClient settingWebClient, TokenService tokenService) {
        this.organizationWebClient = organizationWebClient;
        this.roleWebClient = roleWebClient;
        this.userWebClient = userWebClient;
        this.settingWebClient = settingWebClient;
        this.tokenService = tokenService;
    }

    /*
    get users in the organization id
     */
    @GetMapping
    public Mono<String> getUserForDefaultOrganization(Model model, Pageable userPageable) {
        String accessToken = tokenService.getAccessToken();
        UUID userId = Util.getLoggedInUserId();

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
                    model.addAttribute("authzManagerRoleOrganizationId", uuid);
                })
                .flatMap(stringStringMap -> settingWebClient.getDefaultOrganization(accessToken, userId))
                .switchIfEmpty(Mono.error(new AuthzManagerException(noDefaultOrgFound)))
                .flatMap(orgId -> organizationWebClient.getOrganizationById(accessToken, orgId))
                .doOnNext(organization -> model.addAttribute("organizationId", organization.getId()))
                .doOnNext(organization -> model.addAttribute("organization", organization))
                .flatMap(organization -> organizationWebClient.getUsersInOrganizationId(accessToken, organization.getId(), pageable).zipWith(Mono.just(organization)))
                        .flatMap(orgWithUserIdPage -> {
                            return userWebClient.getUserByBatchOfIds(accessToken, orgWithUserIdPage.getT1().getContent())
                                    .doOnNext(users -> {

                                        LOG.info("add users to model: {}", users);
                                        model.addAttribute("users", users);

                                        LOG.info("add userIdPage to model");
                                        model.addAttribute("page", orgWithUserIdPage.getT1());
                                    }).zipWith(Mono.just(orgWithUserIdPage.getT2()).zipWith(Mono.just(orgWithUserIdPage.getT1())));
                        })
                        .flatMap(usersWithOrgAndUserIdPage ->
                             roleWebClient.areUsersSuperAdminInDefaultOrgId(accessToken,
                                    usersWithOrgAndUserIdPage.getT2().getT1().getId(),
                                    usersWithOrgAndUserIdPage.getT2().getT2().getContent()).zipWith(Mono.just(usersWithOrgAndUserIdPage.getT1()))
                        )
                .doOnNext(uuidBooleanMapWithUserList -> {
                    LOG.info("got uuidBooleanMap {}", uuidBooleanMapWithUserList);
                    List<User> userList = uuidBooleanMapWithUserList.getT2();

                    for(User user: userList) {
                       UUID authzManagerRoleOrganizationId = uuidBooleanMapWithUserList.getT1().get(user.getId());
                        if (authzManagerRoleOrganizationId != null) {
                            user.setAuthzManagerRoleOrganizationId(authzManagerRoleOrganizationId);
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

    @PostMapping
    public Mono<String> setUserSuperAdmin(@RequestParam("authzManagerRoleId")UUID authzManagerRoleId, @RequestParam("userId") UUID targetUserId,
                                          @RequestParam("organizationId")UUID organizationId, Model model, Pageable userPageable) {
        String accessToken = tokenService.getAccessToken();
        UUID loggedInUserId = Util.getLoggedInUserId();

        return roleWebClient.addUserToSuperAdminRoleInOrganization(accessToken, authzManagerRoleId, organizationId, targetUserId, userPageable)
                .doOnNext(stringObjectMap -> {
                    LOG.info("assigned user to superadmin role with a authzManagerRoleOrganiation id {}", stringObjectMap.get("id"));
                })
                .then(getUserForDefaultOrganization(model, userPageable));
    }


}
