package me.sonam.authzmanager.controller.admin.user;

import me.sonam.authzmanager.AuthzManagerException;
import me.sonam.authzmanager.controller.util.Util;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.OrganizationWebClient;
import me.sonam.authzmanager.webclients.SettingWebClient;
import me.sonam.authzmanager.webclients.UserWebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * this is for getting users in the `Add User` menu tab.
 */

//@Controller
//@RequestMapping("/admin/organizations/default/users")
public class UsersController {
    private static final Logger LOG = LoggerFactory.getLogger(UsersController.class);

    private final OrganizationWebClient organizationWebClient;
    private final UserWebClient userWebClient;
    private final TokenService tokenService;
    private final SettingWebClient settingWebClient;

    public UsersController(OrganizationWebClient organizationWebClient,
                                  UserWebClient userWebClient,
                           SettingWebClient settingWebClient, TokenService tokenService) {
        this.organizationWebClient = organizationWebClient;
        this.userWebClient = userWebClient;
        this.settingWebClient = settingWebClient;
        this.tokenService = tokenService;
    }


    /*
    get users in the organization id
     */
    @GetMapping
    public Mono<String> getUserForOrganizationId(Model model, Pageable userPageable) {
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

        String PATH = "/admin/users/list";
        return settingWebClient.getDefaultOrganization(accessToken, userId)
                .switchIfEmpty(Mono.error(new AuthzManagerException(noDefaultOrgFound)))
                .flatMap(orgId -> organizationWebClient.getOrganizationById(accessToken, orgId))
                .doOnNext(organization -> model.addAttribute("organization", organization))
                .flatMap(organization -> organizationWebClient.getUserIdsInOrganizationId(accessToken, organization.getId(), pageable))
                .flatMap(uuidPage -> {
                    LOG.info("uuidPage: {}", uuidPage.content());
                    model.addAttribute("page", uuidPage);
                    return userWebClient.getUserByBatchOfIds(accessToken, uuidPage.content());
                })
                .doOnNext(users -> {
                    LOG.info("got users: {}", users);
                    model.addAttribute("users", users);
                })
                .thenReturn(PATH)
                .onErrorResume(throwable -> {
                    LOG.error("Exception occured", throwable);

                    if (throwable.getMessage().equals(noDefaultOrgFound)) {
                        model.addAttribute("error", "You need to set a default organization to add user.");
                        LOG.info("add error message when default org not found");
                    }
                    else {
                        LOG.info("exception caught with message: {}", throwable.getMessage());
                    }

                    return Mono.just(PATH);
                });
    }

}
