package me.sonam.authzmanager.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import me.sonam.authzmanager.controller.admin.organization.Organization;
import me.sonam.authzmanager.controller.admin.subdomain.Subdomain;
import me.sonam.authzmanager.controller.admin.subdomain.SubdomainOrganizationUser;
import me.sonam.authzmanager.clients.user.ClientOrganization;
import me.sonam.authzmanager.controller.admin.clients.carrier.ClientOrganizationUserWithRole;
import me.sonam.authzmanager.controller.admin.roles.Role;
import me.sonam.authzmanager.clients.user.User;
import me.sonam.authzmanager.controller.util.Util;
import me.sonam.authzmanager.oauth2.AuthorizationGrantType;
import me.sonam.authzmanager.oauth2.OauthClient;
import me.sonam.authzmanager.oauth2.util.RegisteredClientUtil;
import me.sonam.authzmanager.service.ClientLimitService;
import me.sonam.authzmanager.rest.CustomPair;
import me.sonam.authzmanager.rest.RestPage;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.OauthClientWebClient;
import me.sonam.authzmanager.webclients.ClientOrganizationWebClient;
import me.sonam.authzmanager.webclients.OrganizationWebClient;
import me.sonam.authzmanager.webclients.RoleWebClient;
import me.sonam.authzmanager.webclients.UserWebClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

/** JSON facade used by the Next.js AuthzManager UI. Internal service endpoints stay private. */
@RestController
@RequestMapping("/api/admin")
public class AdminApiController {
    private final OauthClientWebClient oauthClientWebClient;
    private final ClientOrganizationWebClient clientOrganizationWebClient;
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
                              ClientOrganizationWebClient clientOrganizationWebClient,
                              OrganizationWebClient organizationWebClient,
                              RoleWebClient roleWebClient,
                              UserWebClient userWebClient,
                              TokenService tokenService,
                              @Value("${openissuer.tenant-host:}") String tenantHost,
                              ClientLimitService clientLimitService) {
        this.oauthClientWebClient = oauthClientWebClient;
        this.clientOrganizationWebClient = clientOrganizationWebClient;
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

    @GetMapping("/clients/{id}/organizations")
    public Mono<ClientOrganizationsResponse> clientOrganizations(@PathVariable UUID id, Pageable pageable) {
        String accessToken = tokenService.getAccessToken();
        Pageable safePageable = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 25), Sort.by("name"));
        return roleWebClient.getOrgAdminOrganizationIdsForUser(accessToken, safePageable)
                .flatMap(ids -> organizationWebClient.getOrganizationByIdsIn(accessToken, ids.content())
                        .flatMap(organizations -> clientOrganizationWebClient
                                .getClientIdOrganizationIdMatch(accessToken, organizations, id)
                                .defaultIfEmpty(new ClientOrganization())
                                .map(selected -> {
                                    UUID selectedId = selected.getOrganization() == null ? null : selected.getOrganization().getId();
                                    List<ClientOrganization> rows = organizations.stream()
                                            .map(org -> new ClientOrganization(id, org, org.getId().equals(selectedId)))
                                            .toList();
                                    return new ClientOrganizationsResponse(id, new RestPage<>(rows, ids.number(), ids.size(), ids.totalElements()));
                                })));
    }

    @PostMapping("/clients/{id}/organizations")
    public Mono<Void> addClientOrganization(@PathVariable UUID id, @RequestBody OrganizationAssociation association) {
        if (association == null || association.organizationId() == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "organizationId is required"));
        }
        return clientOrganizationWebClient.addClientToOrganization(tokenService.getAccessToken(), id, association.organizationId()).then();
    }

    @DeleteMapping("/clients/{id}/organizations/{organizationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> removeClientOrganization(@PathVariable UUID id, @PathVariable UUID organizationId) {
        return clientOrganizationWebClient.deleteClientOrganizationAssociation(tokenService.getAccessToken(), id, organizationId).then();
    }

    @GetMapping("/clients/{id}/users")
    public Mono<ClientUsersResponse> clientUsers(@PathVariable UUID id, Pageable pageable) {
        String accessToken = tokenService.getAccessToken();
        Pageable safePageable = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 25), Sort.by("name"));
        return clientOrganizationWebClient.getOrganizationIdAssociatedWithClientId(accessToken, id)
                .flatMap(organizationId -> organizationWebClient.getOrganizationById(accessToken, organizationId)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found")))
                        .flatMap(organization -> roleWebClient.getRolesByOrganizationId(accessToken, organizationId, PageRequest.of(0, Integer.MAX_VALUE))
                                .zipWith(Mono.just(organization)))
                        .flatMap(rolesAndOrganization -> organizationWebClient.getUserIdsInOrganizationId(accessToken, organizationId, safePageable)
                                .flatMap(userIds -> userWebClient.getUserByBatchOfIds(accessToken, userIds.content())
                                        .defaultIfEmpty(List.of())
                                        .flatMap(users -> roleWebClient.getClientOrganizationUserWithRoles(accessToken, id, organizationId, userIds.content())
                                                .defaultIfEmpty(List.of())
                                                .map(assignments -> new ClientUsersResponse(id, rolesAndOrganization.getT2(), rolesAndOrganization.getT1().content(), userIds, users, assignments))))));
    }

    @PostMapping("/clients/{id}/users/roles")
    public Mono<ClientOrganizationUserWithRole> addClientUserRole(@PathVariable UUID id, @RequestBody ClientOrganizationUserWithRole request) {
        if (request == null || request.getUser() == null || request.getUser().getId() == null || request.getRole() == null || request.getRole().getId() == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "user and role are required"));
        }
        request.setClientId(id);
        return roleWebClient.addClientOrganizationUserRole(tokenService.getAccessToken(), request).thenReturn(request);
    }

    @DeleteMapping("/clients/{id}/users/roles/{assignmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> removeClientUserRole(@PathVariable UUID id, @PathVariable UUID assignmentId) {
        return roleWebClient.deleteClientOrganizationUserRole(tokenService.getAccessToken(), assignmentId).then();
    }

    @GetMapping("/organizations")
    public Mono<RestPage<Organization>> organizations(Pageable pageable) {
        String accessToken = tokenService.getAccessToken();
        Pageable safePageable = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 25), Sort.by("name"));
        return roleWebClient.getOrgAdminOrganizationIdsForUser(accessToken, safePageable)
                .flatMap(ids -> organizationWebClient.getOrganizationByIdsIn(accessToken, ids.content())
                        .map(organizations -> new RestPage<>(organizations, ids.number(), ids.size(), ids.totalElements())));
    }

    @GetMapping("/organizations/{id}")
    public Mono<Organization> organization(@PathVariable UUID id) {
        return organizationWebClient.getOrganizationById(tokenService.getAccessToken(), id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found")));
    }

    @PutMapping("/organizations/{id}")
    public Mono<Organization> updateOrganization(@PathVariable UUID id, @RequestBody Organization organization) {
        if (organization == null || !StringUtils.hasText(organization.getName())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization name is required"));
        }
        organization.setId(id);
        return organizationWebClient.updateOrganization(tokenService.getAccessToken(), organization, HttpMethod.PUT);
    }

    @GetMapping("/organizations/{id}/roles")
    public Mono<RestPage<Role>> organizationRoles(@PathVariable UUID id, Pageable pageable) {
        Pageable safePageable = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 25), Sort.by("name"));
        return roleWebClient.getRolesByOrganizationId(tokenService.getAccessToken(), id, safePageable);
    }

    @PostMapping("/organizations/{id}/roles")
    public Mono<Role> createOrganizationRole(@PathVariable UUID id, @RequestBody Role role) {
        if (role == null || !StringUtils.hasText(role.getName())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role name is required"));
        }
        role.setOrganizationId(id);
        return roleWebClient.updateRole(tokenService.getAccessToken(), role, HttpMethod.POST)
                .onErrorResume(WebClientResponseException.class, error ->
                        Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                error.getResponseBodyAsString(), error)));
    }

    @DeleteMapping("/organizations/{id}/roles/{roleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteOrganizationRole(@PathVariable UUID id, @PathVariable UUID roleId) {
        return roleWebClient.deleteRole(tokenService.getAccessToken(), roleId).then();
    }

    @GetMapping("/organizations/{id}/users")
    public Mono<UserListResponse> organizationUsers(@PathVariable UUID id, Pageable pageable) {
        Pageable safePageable = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 25));
        String accessToken = tokenService.getAccessToken();
        return organizationWebClient.getUserIdsInOrganizationId(accessToken, id, safePageable)
                .flatMap(ids -> userWebClient.getUserByBatchOfIds(accessToken, ids.content())
                        .defaultIfEmpty(List.of())
                        .map(users -> new UserListResponse(ids, users)));
    }

    @PostMapping("/organizations/{id}/users")
    public Mono<Void> updateOrganizationUser(@PathVariable UUID id, @RequestBody OrganizationUserAction action) {
        if (action == null || action.userId() == null || !StringUtils.hasText(action.action())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId and action are required"));
        }
        String accessToken = tokenService.getAccessToken();
        return switch (action.action().toLowerCase()) {
            case "add" -> organizationWebClient.addUserToOrganization(accessToken, action.userId(), id).then();
            case "remove" -> organizationWebClient.removeUserFromOrganization(accessToken, action.userId(), id).then();
            case "default" -> organizationWebClient.setDefaultOrganization(accessToken, id, action.userId()).then();
            default -> Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported action"));
        };
    }

    @PostMapping("/organizations/{id}/users/lookup")
    public Mono<OrganizationUserLookup> lookupOrganizationUser(@PathVariable UUID id, @RequestBody UserLookup lookup,
                                                               HttpServletRequest request) {
        if (lookup == null || !StringUtils.hasText(lookup.authenticationId())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "authenticationId is required"));
        }
        String accessToken = tokenService.getAccessToken();
        return userWebClient.findByAuthenticationProfileSearch(accessToken, lookup.authenticationId())
                .flatMap(user -> organizationWebClient.userExistsInOrganization(accessToken, user.getId(), id)
                        .zipWith(organizationWebClient.getDefaultOrganizationIdForUser(accessToken, user.getId(), effectiveHost(request))
                                .map(Optional::of).defaultIfEmpty(Optional.empty()))
                        .map(result -> new OrganizationUserLookup(user, result.getT1(), result.getT2().map(id::equals).orElse(false))));
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
    public record OrganizationAssociation(UUID organizationId) {}
    public record OrganizationUserAction(UUID userId, String action) {}
    public record UserLookup(String authenticationId) {}
    public record OrganizationUserLookup(User user, boolean selected, boolean defaultOrganization) {}
    public record ClientOrganizationsResponse(UUID clientId, RestPage<ClientOrganization> organizations) {}
    public record ClientUsersResponse(UUID clientId, Organization organization, List<Role> roles,
                                      RestPage<UUID> page, List<User> users,
                                      List<ClientOrganizationUserWithRole> assignments) {}
    public record SubdomainResponse(String host, Subdomain subdomain,
                                    RestPage<Organization> organizations,
                                    RestPage<SubdomainOrganizationUser> users) {}
}
