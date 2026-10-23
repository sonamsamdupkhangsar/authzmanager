package me.sonam.authzmanager.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.sonam.authzmanager.controller.admin.organization.Organization;
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

    public AdminApiController(OauthClientWebClient oauthClientWebClient,
                              OrganizationWebClient organizationWebClient,
                              RoleWebClient roleWebClient,
                              UserWebClient userWebClient,
                              TokenService tokenService) {
        this.oauthClientWebClient = oauthClientWebClient;
        this.organizationWebClient = organizationWebClient;
        this.roleWebClient = roleWebClient;
        this.userWebClient = userWebClient;
        this.tokenService = tokenService;
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
        return organizationWebClient.getDefaultOrganizationIdForUser(accessToken, userId, request.getServerName())
                .flatMap(organizationId -> organizationWebClient.getUserIdsInOrganizationId(
                        accessToken, organizationId, PageRequest.of(0, 100)))
                .flatMap(ids -> userWebClient.getUserByBatchOfIds(accessToken, ids.content())
                        .map(users -> new UserListResponse(ids, users)));
    }

    public record UserListResponse(RestPage<UUID> page, List<?> users) {}
}
