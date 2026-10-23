package me.sonam.authzmanager.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.sonam.authzmanager.controller.admin.organization.Organization;
import me.sonam.authzmanager.controller.admin.subdomain.Subdomain;
import me.sonam.authzmanager.controller.admin.subdomain.SubdomainOrganizationUser;
import me.sonam.authzmanager.controller.util.Util;
import me.sonam.authzmanager.rest.CustomPair;
import me.sonam.authzmanager.rest.RestPage;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.OauthClientWebClient;
import me.sonam.authzmanager.webclients.OrganizationWebClient;
import me.sonam.authzmanager.webclients.RoleWebClient;
import me.sonam.authzmanager.webclients.UserWebClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/** JSON facade used by the Next.js AuthzManager UI. Internal service endpoints stay private. */
@RestController
@RequestMapping("/api/admin")
public class AdminApiController {
    private final OauthClientWebClient oauthClientWebClient;
    private final OrganizationWebClient organizationWebClient;
    private final RoleWebClient roleWebClient;
    private final UserWebClient userWebClient;
    private final TokenService tokenService;
    private final String tenantHost;

    public AdminApiController(OauthClientWebClient oauthClientWebClient,
                              OrganizationWebClient organizationWebClient,
                              RoleWebClient roleWebClient,
                              UserWebClient userWebClient,
                              TokenService tokenService,
                              @Value("${openissuer.tenant-host:}") String tenantHost) {
        this.oauthClientWebClient = oauthClientWebClient;
        this.organizationWebClient = organizationWebClient;
        this.roleWebClient = roleWebClient;
        this.userWebClient = userWebClient;
        this.tokenService = tokenService;
        this.tenantHost = tenantHost;
    }

    @GetMapping("/clients")
    public Mono<RestPage<CustomPair<String, String>>> clients() {
        return oauthClientWebClient.getUserClientIds(
                tokenService.getAccessToken(), Util.getLoggedInUserId(),
                PageRequest.of(0, 100, Sort.by("name")));
    }

    @GetMapping("/organizations")
    public Mono<RestPage<Organization>> organizations() {
        String accessToken = tokenService.getAccessToken();
        return roleWebClient.getOrgAdminOrganizationIdsForUser(accessToken, PageRequest.of(0, 100, Sort.by("name")))
                .flatMap(ids -> organizationWebClient.getOrganizationByIdsIn(accessToken, ids.content())
                        .map(organizations -> new RestPage<>(organizations, ids.number(), ids.size(), ids.totalElements())));
    }

    @GetMapping("/users")
    public Mono<UserListResponse> users(HttpServletRequest request) {
        String accessToken = tokenService.getAccessToken();
        UUID userId = Util.getLoggedInUserId();
        return organizationWebClient.getDefaultOrganizationIdForUser(accessToken, userId, effectiveHost(request))
                .flatMap(organizationId -> organizationWebClient.getUserIdsInOrganizationId(
                        accessToken, organizationId, PageRequest.of(0, 100)))
                .flatMap(ids -> userWebClient.getUserByBatchOfIds(accessToken, ids.content())
                        .map(users -> new UserListResponse(ids, users)));
    }

    @GetMapping("/subdomain")
    public Mono<SubdomainResponse> subdomain(HttpServletRequest request) {
        String accessToken = tokenService.getAccessToken();
        String host = effectiveHost(request);
        return organizationWebClient.getSubdomainByHost(accessToken, host)
                .flatMap(subdomain -> Mono.zip(
                        organizationWebClient.getOrganizationsBySubdomain(accessToken, host, PageRequest.of(0, 100)),
                        organizationWebClient.getUsersBySubdomain(accessToken, host, PageRequest.of(0, 100)))
                        .map(result -> new SubdomainResponse(host, subdomain, result.getT1(), result.getT2())));
    }

    private String effectiveHost(HttpServletRequest request) {
        return tenantHost == null || tenantHost.isBlank() ? request.getServerName() : tenantHost;
    }

    public record UserListResponse(RestPage<UUID> page, List<?> users) {}
    public record SubdomainResponse(String host, Subdomain subdomain,
                                    RestPage<Organization> organizations,
                                    RestPage<SubdomainOrganizationUser> users) {}
}
