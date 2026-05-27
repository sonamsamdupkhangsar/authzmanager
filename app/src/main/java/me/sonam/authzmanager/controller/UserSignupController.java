package me.sonam.authzmanager.controller;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import me.sonam.authzmanager.controller.admin.organization.Organization;
import me.sonam.authzmanager.controller.signup.UserSignup;
import me.sonam.authzmanager.controller.util.Util;
import me.sonam.authzmanager.webclients.OrganizationWebClient;
import me.sonam.authzmanager.webclients.UserWebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

/**
 * This controller is for signing up user using the user-rest-service and other microservices
 *//*
@Controller
@RequestMapping("/signup")*/
public class UserSignupController {
    private static final Logger LOG = LoggerFactory.getLogger(UserSignupController.class);
    @Autowired
    private final UserWebClient userWebClient;

    @Autowired
    private OrganizationWebClient organizationWebClient;

    public UserSignupController(UserWebClient userWebClient) {
        this.userWebClient = userWebClient;
    }
    @GetMapping
    public Mono<String> getSignupForm(Model model) {
        final String PATH = "signupform";
        LOG.info("returning {}", PATH);

        model.addAttribute("userSignup", new UserSignup());
        return Mono.just(PATH);
    }

    public Mono<String> userSignupByAdmin(String accessToken, UserSignup userSignup, BindingResult bindingResult,
                                          Model model, final String PATH, HttpServletRequest request) {
        LOG.info("User signup initiated by admin for user: {}", userSignup);

        if (bindingResult.hasErrors()) {
            LOG.info("required fields missing for user {}", bindingResult.getAllErrors());
            model.addAttribute("error", "Data validation failed");
            return Mono.just(PATH);
        }
        String subdomain = request.getServerName();
        return organizationWebClient.canAddUserToOrganization(accessToken, userSignup.getOrganizationId(), subdomain)
                .then(userWebClient.findByAuthenticationId(accessToken, userSignup.getAuthenticationId())
                        .onErrorResume(throwable -> Mono.empty())
                        .flatMap(user -> organizationWebClient.canAddUserToOrganization(accessToken, user.getId(),
                                userSignup.getOrganizationId(), subdomain)))
                .then(userWebClient.signupUser(accessToken, userSignup))
                .flatMap(s -> {
                    LOG.info("user has been added successfully with message: {}", s);
                    StringBuilder stringBuilder = new StringBuilder(userSignup.getFirstName())
                            .append(" ").append(userSignup.getLastName())
                            .append(" has been added successfully! ");

                    if (!userSignup.isActive()) {
                        stringBuilder.append("Let the user know they will get a email to activate")
                                .append(" their account");
                    } else {
                        stringBuilder.append("Their account is now active and can log-in with the ")
                                .append("password that was set by you.");
                    }

                    model.addAttribute("message", stringBuilder.toString());
                    return Mono.just(PATH);
                })
                .flatMap(s -> userWebClient.findByAuthenticationId(accessToken, userSignup.getAuthenticationId()))
                .flatMap(user -> organizationWebClient.addUserToOrganization(accessToken, user.getId(),
                        userSignup.getOrganizationId(), subdomain, true))
                .thenReturn(PATH)
                .onErrorResume(throwable -> {
                    LOG.info("exception occured in signing up user by admin {}", throwable.getMessage());
                    setErrorInModel(throwable, model, "failed to add user by admin");
                    model.addAttribute("userSignup", userSignup);
                    return Mono.just(PATH);
                });

    }


    private void setErrorInModel(Throwable throwable, Model model, String defaultErrMessage) {
        LOG.error("exception occured in signup user", throwable);
        LOG.error(defaultErrMessage);

        if (throwable instanceof WebClientResponseException webClientResponseException) {
            Map<String, String> map = webClientResponseException.getResponseBodyAs(
                    new ParameterizedTypeReference<>() {});

            if (map != null) {
                LOG.error("{}: {}", defaultErrMessage, map.get("error"));

                if (map.get("error") != null) {
                    model.addAttribute("error", map.get("error"));
                }
                else {
                    LOG.error("there is no error key in the map, add map itself to error message");
                    model.addAttribute("error", map);
                }
            }
            else {
                LOG.error("map is null on response for throwable", throwable);
                model.addAttribute("error", defaultErrMessage + throwable.getMessage());
            }
            LOG.error("{}: {}", defaultErrMessage, throwable.getMessage());
        } else {
            //set model error attribute to present back to user
            model.addAttribute("error", defaultErrMessage  + throwable.getMessage());
        }
    }


}
