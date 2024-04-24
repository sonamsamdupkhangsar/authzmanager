package me.sonam.authzmanager.controller;

import jakarta.validation.Valid;
import me.sonam.authzmanager.clients.user.UserWebClient;
import me.sonam.authzmanager.controller.signup.UserSignup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

/**
 * This controller is for signing up user using the user-rest-service and other microservices
 */
@Controller
@RequestMapping("/signup")
public class UserSignupController {
    private static final Logger LOG = LoggerFactory.getLogger(UserSignupController.class);
    @Autowired
    private UserWebClient userWebClient;

    public UserSignupController(UserWebClient userWebClient) {
        this.userWebClient = userWebClient;
    }
    @GetMapping
    public Mono<String> getSignupForm(Model model) {
        final String PATH = "signupform";
        LOG.info("returning {}", PATH);

        model.addAttribute("signupUser", new UserSignup());
        return Mono.just(PATH);
    }

    @PostMapping
    public Mono<String> signupUserFromForm(@Valid @ModelAttribute("signupUser") UserSignup userSignup,
                                           BindingResult bindingResult, Model model) {
        final String PATH = "signupform";
        LOG.info("signing up user: {}", userSignup);

        if (bindingResult.hasErrors()) {
            LOG.info("user didn't enter required fields");
            model.addAttribute("error", "Data validation failed");
         //   model.addAttribute("signupUser", userSignup);
            return Mono.just(PATH);
        }
        return userWebClient.signupUser(userSignup)
                .flatMap(s -> {
                    LOG.info("user signup successful with message: {}",s);
                    StringBuilder stringBuilder = new StringBuilder(userSignup.getFirstName())
                            .append(", your signup was successful!").append(
                                    " Please check your email '").append(userSignup.getEmail())
                            .append("' to activate your account.");

                    model.addAttribute("message", stringBuilder.toString());
                    return Mono.just(PATH);
                })
                .onErrorResume(throwable -> {
                    if (throwable instanceof WebClientResponseException) {
                        WebClientResponseException webClientResponseException = (WebClientResponseException) throwable;
                        LOG.error("error body contains: {}", webClientResponseException.getResponseBodyAsString());
                        if (webClientResponseException.getResponseBodyAsString().contains("\"error\":")) {
                            String error = webClientResponseException.getResponseBodyAs(Map.class).get("error").toString();
                            model.addAttribute("error", error);
                            LOG.error("error occured: {}", error);
                        }
                        else {
                            LOG.error("error occured: {}", webClientResponseException.getResponseBodyAsString());
                            model.addAttribute("error", webClientResponseException.getResponseBodyAsString());
                        }
                    }
                    else {
                        LOG.error("failed to signup user", throwable);
                        LOG.info("exception is not a WebClientResponseException type");
                        model.addAttribute("error", "failed to signup user: " + throwable.getMessage());
                    }
                    model.addAttribute("signupUser", userSignup);
                    return Mono.just(PATH);
                });

    }




}
