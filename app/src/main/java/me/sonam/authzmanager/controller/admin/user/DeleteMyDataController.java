package me.sonam.authzmanager.controller.admin.user;

import jakarta.servlet.http.HttpServletRequest;
import me.sonam.authzmanager.AuthzManagerException;
import me.sonam.authzmanager.controller.util.MessageConstants;
import me.sonam.authzmanager.controller.util.Util;
import me.sonam.authzmanager.tenant.TenantAuthorizationUrlResolver;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.*;
import org.apache.tomcat.websocket.AuthenticationException;
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
    private final OrganizationWebClient organizationWebClient;
    private final RoleWebClient roleWebClient;
    private final TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver;

    public DeleteMyDataController(UserWebClient userWebClient, OauthClientWebClient oauthClientWebClient,
                                  TokenService tokenService, OrganizationWebClient organizationWebClient,
                                  RoleWebClient roleWebClient,
                                  TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver) {
        this.userWebClient = userWebClient;
        this.oauthClientWebClient = oauthClientWebClient;
        this.tokenService = tokenService;
        this.organizationWebClient = organizationWebClient;
        this.roleWebClient = roleWebClient;
        this.tenantAuthorizationUrlResolver = tenantAuthorizationUrlResolver;
    }

    @GetMapping
    public Mono<String> deleteMe(Model model) {
        LOG.info("get delete my data ui");

        return Mono.just(PATH);
    }

    @DeleteMapping
    public Mono<String> deleteMyInfo(Model model, HttpServletRequest request) {
        DefaultOidcUser defaultOidcUser = (DefaultOidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userIdString = defaultOidcUser.getAttribute("userId");

        LOG.info("delete user data for userId: {}", userIdString);
        final String accessToken = tokenService.getAccessToken();
        LOG.info("accessToken {}", accessToken);
        UUID userId = Util.getLoggedInUserId();
        String organizationHost = tenantAuthorizationUrlResolver.currentAuthorizationHost();

        return organizationWebClient.getDefaultOrganizationIdForUser(accessToken, userId, organizationHost)
                .switchIfEmpty(Mono.error(new AuthzManagerException("no default organization found")))
                .flatMap(orgId -> roleWebClient.isOrgAdminInOrgId(accessToken, userId, orgId).zipWith(Mono.just(orgId)))
                .flatMap(objects -> {
                    if (!objects.getT1()) {
                        model.addAttribute("error", MessageConstants.NOT_ORG_ADMIN + " " + objects.getT2());
                        return Mono.error(new AuthenticationException(MessageConstants.NOT_ORG_ADMIN));
                    }
                    return oauthClientWebClient.deleteClient(accessToken).then(
                            userWebClient.deleteUser(accessToken, objects.getT2())).thenReturn(PATH);
                });
    }

}
