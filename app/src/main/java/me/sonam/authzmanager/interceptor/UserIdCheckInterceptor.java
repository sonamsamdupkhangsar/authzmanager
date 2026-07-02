package me.sonam.authzmanager.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.UserWebClient;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.web.servlet.HandlerInterceptor;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class UserIdCheckInterceptor implements HandlerInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(UserIdCheckInterceptor.class);

    private final TokenService tokenService;
    private final UserWebClient userWebClient;

    public UserIdCheckInterceptor(TokenService tokenService, UserWebClient userWebClient) {
        this.tokenService = tokenService;
        this.userWebClient = userWebClient;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        Object principalObject = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principalObject != null) {
            if (principalObject.toString().equals("anonymousUser")) {

            }
            else {
                try {
                    DefaultOidcUser defaultOidcUser = (DefaultOidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                    String userIdString = defaultOidcUser.getAttribute("userId");
                    LOG.info("userIdString: {}", userIdString);
                    if (userIdString == null) {
                        LOG.error("userIdString is null");
                    }
                    else {
                        UUID userId = UUID.fromString(userIdString);

                        final String accessToken = tokenService.getAccessToken();
                        LOG.info("validate authenticated userId {}", userId);

                        LOG.info("get user by id to verify the user exists in user-rest-service");//in case it was deleted or does not exist
                        userWebClient.getUserById(accessToken, userId)
                                .doOnNext(user -> {
                                    LOG.info("found user {}", user);
                                })
                                .onErrorResume(throwable -> {
                                    LOG.error("error occurred in getUserById check for userId {}, {}", userId, throwable.getMessage());
                                    LOG.debug("exception stacktrace", throwable);
                                    return Mono.error(new InvalidUserIdException("userId invalid"));}).block();

                    }
                }
                catch (Exception e) {
                    LOG.error("Failed to get oidcUser, error: {}", e.getMessage());
                }
            }
        }
        LOG.info("principalObject class {}, principalObject: {}", principalObject.getClass(), principalObject);




        return true;
    }
}
