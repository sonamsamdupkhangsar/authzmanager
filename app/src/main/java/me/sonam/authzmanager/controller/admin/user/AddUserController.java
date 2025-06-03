package me.sonam.authzmanager.controller.admin.user;

import jakarta.validation.Valid;
import me.sonam.authzmanager.AuthzManagerException;
import me.sonam.authzmanager.controller.UserSignupController;
import me.sonam.authzmanager.controller.admin.organization.Organization;
import me.sonam.authzmanager.controller.signup.UserSignup;

import me.sonam.authzmanager.controller.util.Util;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.OrganizationWebClient;
import me.sonam.authzmanager.webclients.SettingWebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Controller
@RequestMapping("/admin/users")
public class AddUserController {
    private static final Logger LOG = LoggerFactory.getLogger(AddUserController.class);

    private final UserSignupController userSignupController;
    private final TokenService tokenService;
    private final SettingWebClient settingWebClient;
    private final OrganizationWebClient organizationWebClient;
    private final String USER_ADD = "/admin/users/add";

    public AddUserController(UserSignupController userSignupController, SettingWebClient settingWebClient, OrganizationWebClient organizationWebClient,
                             TokenService tokenService) {
        this.userSignupController = userSignupController;
        this.tokenService = tokenService;
        this.settingWebClient = settingWebClient;
        this.organizationWebClient = organizationWebClient;
    }

    @GetMapping
    public Mono<String> getSignupForm(Model model) {
        LOG.info("returning {}", USER_ADD);

        final String accessToken = tokenService.getAccessToken();

        return settingWebClient.getDefaultOrganization(accessToken, Util.getLoggedInUserId())
                .switchIfEmpty(Mono.error(new AuthzManagerException("no default organization found")))
                .flatMap(uuid -> organizationWebClient.getOrganizationById(accessToken, uuid))
                .doOnNext(organization -> model.addAttribute("organization", organization))
                        .doOnNext(organization -> model.addAttribute("signupUser", new UserSignup()))
                .thenReturn(USER_ADD)
                .onErrorResume(throwable -> {
                    LOG.debug("error occurred in setting defaultOrganization", throwable);
                    LOG.error("failed to set default organization {}", throwable.getMessage());

                    model.addAttribute("organization", new Organization());
                    model.addAttribute("signupUser", new UserSignup());
                    return Mono.just(USER_ADD);
                });
    }

    @PostMapping
    public Mono<String> signupUserFromForm(@Valid @ModelAttribute("signupUser") UserSignup userSignup,
        BindingResult bindingResult, Model model) {

        LOG.info("delegate user add to userSignupController for userSignup {}", userSignup);

        final String PATH = "admin/clients/form";

        final String accessToken = tokenService.getAccessToken();
        userSignup.setAuthenticationId(userSignup.getEmail());

        return organizationWebClient.getOrganizationById(accessToken, userSignup.getOrganizationId())
                .doOnNext(organization -> model.addAttribute("organization", organization))
                        .flatMap(organization -> userSignupController.userSignupByAdmin(accessToken, userSignup,
                                bindingResult, model, USER_ADD));
    }

}
