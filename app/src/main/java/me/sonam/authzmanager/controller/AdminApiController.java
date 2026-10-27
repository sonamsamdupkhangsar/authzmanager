package me.sonam.authzmanager.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import me.sonam.authzmanager.controller.admin.organization.Organization;
import me.sonam.authzmanager.controller.admin.subdomain.Subdomain;
import me.sonam.authzmanager.controller.admin.subdomain.SubdomainOrganizationUser;
import me.sonam.authzmanager.controller.util.Util;
import me.sonam.authzmanager.oauth2.AuthorizationGrantType;
import me.sonam.authzmanager.oauth2.OauthClient;
import me.sonam.authzmanager.oauth2.util.RegisteredClientUtil;
import me.sonam.authzmanager.service.ClientLimitService;
import me.sonam.authzmanager.rest.CustomPair;
import me.sonam.authzmanager.rest.RestPage;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.OauthClientWebClient;
import me.sonam.authzmanager.webclients.OrganizationWebClient;
import me.sonam.authzmanager.webclients.RoleWebClient;
import me.sonam.authzmanager.webclients.UserWebClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
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
    private final ClientLimitService clientLimitService;
    private final RegisteredClientUtil registeredClientUtil = new RegisteredClientUtil();
    @Value("${authzmanager.oauth2.prepend-uuid-to-created-client-id:true}")
    private boolean prependUuidToCreatedClientId;
    @Value("${authzmanager.oauth2.prepend-uuid-to-created-client-id-hosts:}")
    private List<String> prependUuidToCreatedClientIdHosts = List.of();

    public AdminApiController(OauthClientWebClient oauthClientWebClient,
                              OrganizationWebClient organizationWebClient,
                              RoleWebClient roleWebClient,
                              UserWebClient userWebClient,
                              TokenService tokenService,
                              @Value("${openissuer.tenant-host:}") String tenantHost,
                              ClientLimitService clientLimitService) {
        this.oauthClientWebClient = oauthClientWebClient;
        this.organizationWebClient = organizationWebClient;
        this.roleWebClient = roleWebClient;
        this.userWebClient = userWebClient;
        this.tokenService = tokenService;
        this.tenantHost = tenantHost;
        this.clientLimitService = clientLimitService;
    }

    @GetMapping("/clients")
    public Mono<RestPage<CustomPair<String, String>>> clients(Pageable pageable) {
        return oauthClientWebClient.getUserClientIds(
                tokenService.getAccessToken(), Util.getLoggedInUserId(),
                PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 25), Sort.by("name")));
    }

    @GetMapping("/clients/{id}")
    public Mono<OauthClient> client(@PathVariable UUID id) {
        return oauthClientWebClient.getOauthClientById(tokenService.getAccessToken(), id)
                .map(OauthClient::getFromRegisteredClient);
    }

    @PostMapping("/clients")
    public Mono<OauthClient> saveClient(@Valid @RequestBody OauthClient client, HttpServletRequest request) {
        if (client.getAuthorizationGrantTypes() == null || client.getAuthorizationGrantTypes().isEmpty()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Need to select at least one authorization grant type"));
        }
        if (client.getAuthorizationGrantTypes().contains(AuthorizationGrantType.AUTHORIZATION_CODE.getValue().toUpperCase())
                && !StringUtils.hasText(client.getRedirectUris())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "redirect uris is needed for AuthorizationGrantType of AUTHORIZATION_CODE"));
        }
        boolean newClient = !StringUtils.hasText(client.getId());
        if (newClient) {
            client.setTokenSettings(null);
            client.setClientSettings(null);
            client.setPrependUuidToClientId(prependUuidToCreatedClientId(request));
            if (client.isPrependUuidToClientId() && client.getClientIdUuid() == null) {
                client.setClientIdUuid(UUID.randomUUID());
            }
        }
        String host = effectiveHost(request);
        String accessToken = tokenService.getAccessToken();
        java.util.Map<String, Object> map = registeredClientUtil.getMapObject(client.getRegisteredClient());
        map.put("userId", Util.getLoggedInUserId().toString());
        Mono<Boolean> underLimit = newClient
                ? oauthClientWebClient.getDefaultOrganizationClientCount(accessToken)
                    .map(count -> count < clientLimitService.maxClientsForHost(host))
                : Mono.just(true);
        return underLimit.flatMap(ok -> {
            if (!ok) return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Max number of clients reached"));
            return oauthClientWebClient.updateClient(accessToken, map, host);
        }).map(OauthClient::getFromRegisteredClient);
    }

    @DeleteMapping("/clients/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteClient(@PathVariable UUID id) {
        return oauthClientWebClient.deleteClient(tokenService.getAccessToken(), id, Util.getLoggedInUserId());
    }

    @GetMapping("/organizations")
    public Mono<RestPage<Organization>> organizations(Pageable pageable) {
        String accessToken = tokenService.getAccessToken();
        Pageable safePageable = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 25), Sort.by("name"));
        return roleWebClient.getOrgAdminOrganizationIdsForUser(accessToken, safePageable)
                .flatMap(ids -> organizationWebClient.getOrganizationByIdsIn(accessToken, ids.content())
                        .map(organizations -> new RestPage<>(organizations, ids.number(), ids.size(), ids.totalElements())));
    }

    @GetMapping("/users")
    public Mono<UserListResponse> users(HttpServletRequest request, Pageable pageable) {
        String accessToken = tokenService.getAccessToken();
        UUID userId = Util.getLoggedInUserId();
        Pageable safePageable = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 25));
        return organizationWebClient.getDefaultOrganizationIdForUser(accessToken, userId, effectiveHost(request))
                .flatMap(organizationId -> organizationWebClient.getUserIdsInOrganizationId(
                        accessToken, organizationId, safePageable))
                .flatMap(ids -> userWebClient.getUserByBatchOfIds(accessToken, ids.content())
                        .map(users -> new UserListResponse(ids, users)));
    }

    @GetMapping("/subdomain")
    public Mono<SubdomainResponse> subdomain(HttpServletRequest request, Pageable pageable) {
        String accessToken = tokenService.getAccessToken();
        String host = effectiveHost(request);
        Pageable safePageable = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 25));
        return organizationWebClient.getSubdomainByHost(accessToken, host)
                .flatMap(subdomain -> Mono.zip(
                        organizationWebClient.getOrganizationsBySubdomain(accessToken, host, safePageable),
                        organizationWebClient.getUsersBySubdomain(accessToken, host, safePageable))
                        .map(result -> new SubdomainResponse(host, subdomain, result.getT1(), result.getT2())));
    }

    private String effectiveHost(HttpServletRequest request) {
        return tenantHost == null || tenantHost.isBlank() ? request.getServerName() : tenantHost;
    }

    private boolean prependUuidToCreatedClientId(HttpServletRequest request) {
        List<String> hosts = prependUuidToCreatedClientIdHosts.stream()
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .filter(host -> !host.isEmpty())
                .map(String::toLowerCase)
                .toList();
        if (hosts.isEmpty()) return prependUuidToCreatedClientId;
        String requestHost = request == null ? null : request.getServerName();
        return requestHost != null && hosts.contains(requestHost.toLowerCase());
    }

    public record UserListResponse(RestPage<UUID> page, List<?> users) {}
    public record SubdomainResponse(String host, Subdomain subdomain,
                                    RestPage<Organization> organizations,
                                    RestPage<SubdomainOrganizationUser> users) {}
}
