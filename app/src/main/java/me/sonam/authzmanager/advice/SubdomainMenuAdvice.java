package me.sonam.authzmanager.advice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import me.sonam.authzmanager.tenant.TenantAuthorizationUrlResolver;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.OrganizationWebClient;
import me.sonam.authzmanager.webclients.RoleWebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@ControllerAdvice
public class SubdomainMenuAdvice {
    private static final Logger LOG = LoggerFactory.getLogger(SubdomainMenuAdvice.class);
    private static final Duration ROLE_CHECK_TIMEOUT = Duration.ofSeconds(2);
    private static final String SHOW_SUBDOMAIN_MENU_SESSION_ATTRIBUTE = "showSubdomainMenu";

    private final OrganizationWebClient organizationWebClient;
    private final RoleWebClient roleWebClient;
    private final TokenService tokenService;
    private final TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver;

    public SubdomainMenuAdvice(OrganizationWebClient organizationWebClient,
                               RoleWebClient roleWebClient,
                               TokenService tokenService,
                               TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver) {
        this.organizationWebClient = organizationWebClient;
        this.roleWebClient = roleWebClient;
        this.tokenService = tokenService;
        this.tenantAuthorizationUrlResolver = tenantAuthorizationUrlResolver;
    }

    @ModelAttribute("showSubdomainMenu")
    public boolean showSubdomainMenu(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object cached = session.getAttribute(SHOW_SUBDOMAIN_MENU_SESSION_ATTRIBUTE);
            if (cached instanceof Boolean showSubdomainMenu) {
                return showSubdomainMenu;
            }
        }

        if (!shouldLoadSubdomainMenuFlag(request)) {
            return false;
        }

        UUID userId = getLoggedInUserId();
        if (userId == null) {
            return false;
        }

        String accessToken = tokenService.getAccessToken();
        if (accessToken == null) {
            return false;
        }

        String host = tenantAuthorizationUrlResolver.currentAuthorizationHost();
        boolean showSubdomainMenu = organizationWebClient.getSubdomainByHost(accessToken, host)
                .flatMap(subdomain -> roleWebClient.isSubdomainAdminInSubdomainId(accessToken, userId, subdomain.getId()))
                .timeout(ROLE_CHECK_TIMEOUT)
                .onErrorResume(throwable -> {
                    LOG.debug("hide Subdomain menu because SubdomainAdmin check failed", throwable);
                    return Mono.just(false);
                })
                .blockOptional()
                .orElse(false);

        request.getSession().setAttribute(SHOW_SUBDOMAIN_MENU_SESSION_ATTRIBUTE, showSubdomainMenu);
        return showSubdomainMenu;
    }

    private boolean shouldLoadSubdomainMenuFlag(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return requestUri != null && requestUri.startsWith("/admin/");
    }

    private UUID getLoggedInUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof DefaultOidcUser oidcUser)) {
            return null;
        }

        String userIdString = oidcUser.getAttribute("userId");
        if (userIdString == null || userIdString.isBlank()) {
            return null;
        }
        return UUID.fromString(userIdString);
    }
}
