package me.sonam.authzmanager.controller.admin.user;

import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.AccountWebClient;
import me.sonam.authzmanager.webclients.OauthClientWebClient;
import me.sonam.authzmanager.webclients.UserWebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Controller
@RequestMapping("/admin/users/delete")
public class DeleteMyDataController {
    private static final Logger LOG = LoggerFactory.getLogger(DeleteMyDataController.class);

    private static final String PATH = "/admin/user/delete";
    private final TokenService tokenService;
    private final OauthClientWebClient oauthClientWebClient;
    private final UserWebClient userWebClient;

    public DeleteMyDataController(UserWebClient userWebClient, OauthClientWebClient oauthClientWebClient,
                                  TokenService tokenService) {
        this.userWebClient = userWebClient;
        this.oauthClientWebClient = oauthClientWebClient;
        this.tokenService = tokenService;
    }

    @GetMapping
    public Mono<String> deleteMe(Model model) {
        LOG.info("get delete my data ui");

        return Mono.just(PATH);
    }

    @DeleteMapping
    public Mono<String> deleteMyInfo(Model model) {
        DefaultOidcUser defaultOidcUser = (DefaultOidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userIdString = defaultOidcUser.getAttribute("userId");

        LOG.info("delete user data for userId: {}", userIdString);
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String accessToken = tokenService.getAccessToken();
        LOG.info("accessToken {}", accessToken);

        return oauthClientWebClient.deleteClient(accessToken).then(
                userWebClient.deleteUser(accessToken)).thenReturn(PATH);
    }

}
