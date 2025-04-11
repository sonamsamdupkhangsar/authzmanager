package me.sonam.authzmanager.controller.admin.user;

import jakarta.validation.Valid;
import me.sonam.authzmanager.controller.UserSignupController;
import me.sonam.authzmanager.controller.signup.UserSignup;

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

@Controller
@RequestMapping("/admin/users")
public class AddUserController {
    private static final Logger LOG = LoggerFactory.getLogger(AddUserController.class);

    private UserSignupController userSignupController;
    private final String USER_ADD = "/admin/users/add";

    public AddUserController(UserSignupController userSignupController) {
        this.userSignupController = userSignupController;
    }

    @GetMapping
    public Mono<String> getSignupForm(Model model) {
        LOG.info("returning {}", USER_ADD);

        model.addAttribute("signupUser", new UserSignup());
        return Mono.just(USER_ADD);
    }

    @PostMapping
    public Mono<String> signupUserFromForm(@Valid @ModelAttribute("signupUser") UserSignup userSignup,
        BindingResult bindingResult, Model model) {

        LOG.info("delegate user add to userSignupController for userSignup {}", userSignup);

        userSignup.setAuthenticationId(userSignup.getEmail());
        return userSignupController.userSignupByAdmin(userSignup, bindingResult, model, USER_ADD);
    }

}
