package me.sonam.authzmanager.controller.admin.user;

import me.sonam.authzmanager.clients.user.User;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.user.UserId;
import me.sonam.authzmanager.webclients.UserWebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Controller
@RequestMapping("/admin/users")
public class ProfileController {
    private static final Logger LOG = LoggerFactory.getLogger(ProfileController.class);

    private UserWebClient userWebClient;
    private static final String PATH = "/admin/user/profile";
    private TokenService tokenService;

    public ProfileController(UserWebClient userWebClient, TokenService tokenService) {
        this.userWebClient = userWebClient;
        this.tokenService = tokenService;
    }

    @GetMapping
    public Mono<String> getProfile(Model model) {
        LOG.info("get profile for the logged in user");

        final String accessToken = tokenService.getAccessToken();

        return userWebClient.getUserById(accessToken, getUserId())
                .doOnNext(user -> {
                    LOG.info("got user: {}", user);
            model.addAttribute("user", user);
        }).thenReturn(PATH);
    }

    @PostMapping
    public Mono<String> updateProfile(User user, Model model) {
        LOG.info("update profile for the logged in user: {}", user);
        final String accessToken = tokenService.getAccessToken();

        return userWebClient.updateProfile(accessToken, user)
                .doOnNext(s -> LOG.info("updated profile: {}", s))
                .flatMap(s -> userWebClient.getUserById(accessToken, user.getId()))
                .doOnNext(user1 -> {
                    LOG.info("got user: {}", user1);
                    model.addAttribute("user", user1);
                })
                .thenReturn(PATH);
    }

    public UUID getUserId() {
        DefaultOidcUser defaultOidcUser = (DefaultOidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userIdString = defaultOidcUser.getAttribute("userId");
        UUID userId = UUID.fromString(userIdString);
        return userId;
    }
}
