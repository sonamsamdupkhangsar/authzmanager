package me.sonam.authzmanager.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.OrganizationWebClient;
import me.sonam.authzmanager.webclients.RoleWebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.web.servlet.HandlerInterceptor;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class UserGroupInterceptor implements HandlerInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(UserGroupInterceptor.class);

    private final TokenService tokenService;
    private final RoleWebClient roleWebClient;

    private final OrganizationWebClient organizationWebClient;

    public UserGroupInterceptor(TokenService tokenService, RoleWebClient roleWebClient,
                                OrganizationWebClient organizationWebClient) {
        this.tokenService = tokenService;
        this.roleWebClient = roleWebClient;
        this.organizationWebClient = organizationWebClient;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        final boolean[] isUserAdmin = {true};

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
                        LOG.info("userId: {}, token {}", userId, accessToken);

/*
                        organizationWebClient.getDefaultOrganizationIdForUser(accessToken, userId, request.getServerName())
                                        .flatMap(orgId -> roleWebClient.isSuperAdminInOrgId(accessToken, userId, orgId).zipWith(Mono.just(orgId)))
                                .doOnNext(objects -> request.setAttribute("organizationId", objects.getT2()))
                                                .subscribe(objects -> {
                                                    isUserAdmin[0] = objects.getT1();
                                                    LOG.info("user is superadmin in the defaultOrg? {}", objects.getT1());
                                                });*/

                    }
                }
                catch (Exception e) {
                    LOG.error("Failed to get oidcUser, error: {}", e.getMessage());
                }
            }
        }
        LOG.info("principalObject class {}, principalObject: {}", principalObject.getClass(), principalObject);

        return isUserAdmin[0];
    }
}
