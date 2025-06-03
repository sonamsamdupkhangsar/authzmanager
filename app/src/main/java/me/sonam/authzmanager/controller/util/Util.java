package me.sonam.authzmanager.controller.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class Util {
    private static final Logger LOG = LoggerFactory.getLogger(Util.class);

    public static UUID getLoggedInUserId() {
        DefaultOidcUser oidcUser = (DefaultOidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userIdString = oidcUser.getAttribute("userId");
        LOG.info("oidc.userId: {}", userIdString);
        if (userIdString != null) {
            return UUID.fromString(userIdString);
        }
        else {
            return null;
        }
    }
}
