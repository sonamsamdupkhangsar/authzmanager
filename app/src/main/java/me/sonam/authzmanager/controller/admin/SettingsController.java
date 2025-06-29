package me.sonam.authzmanager.controller.admin;

import me.sonam.authzmanager.controller.UserSignupController;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.OrganizationWebClient;
import me.sonam.authzmanager.webclients.SettingWebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/admin/settings")
public class SettingsController {
    private static final Logger LOG = LoggerFactory.getLogger(SettingsController.class);


    private final UserSignupController userSignupController;
    private final TokenService tokenService;
    private final SettingWebClient settingWebClient;
    private final OrganizationWebClient organizationWebClient;
    private final String settingsPage = "/admin/setting/account";

    public SettingsController(UserSignupController userSignupController, SettingWebClient settingWebClient, OrganizationWebClient organizationWebClient,
                             TokenService tokenService) {
        this.userSignupController = userSignupController;
        this.tokenService = tokenService;
        this.settingWebClient = settingWebClient;
        this.organizationWebClient = organizationWebClient;
    }

    @GetMapping
    public Mono<String> getDefaultPage(Model model) {
        LOG.info("returning {}", settingsPage);


        return Mono.just(settingsPage);
    }
}
