package me.sonam.authzmanager.controller.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class Util {
    private static final Logger LOG = LoggerFactory.getLogger(Util.class);

    public static UUID getLoggedInUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String userIdString;
        if (authentication.getPrincipal() instanceof DefaultOidcUser oidcUser) {
            userIdString = oidcUser.getAttribute("userId");
        }
        else if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            userIdString = jwtAuthentication.getToken().getClaimAsString("userId");
        }
        else {
            LOG.warn("unsupported authentication principal: {}", authentication.getPrincipal());
            return null;
        }
        LOG.info("oidc.userId: {}", userIdString);
        if (userIdString != null) {
            return UUID.fromString(userIdString);
        }
        else {
            return null;
        }
    }
}
