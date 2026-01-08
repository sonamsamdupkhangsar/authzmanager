package me.sonam.authzmanager.webclients;

import jakarta.ws.rs.BadRequestException;
import me.sonam.authzmanager.clients.user.ClientOrganizationUserRole;
import me.sonam.authzmanager.controller.admin.roles.RoleOrganization;
import me.sonam.authzmanager.controller.admin.clients.carrier.ClientOrganizationUserWithRole;
import me.sonam.authzmanager.controller.admin.roles.Role;
import me.sonam.authzmanager.rest.RestPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RoleWebClient {
    private static final Logger LOG = LoggerFactory.getLogger(RoleWebClient.class);

    private final WebClient.Builder webClientBuilder;
    private final String roleEndpoint;

    public RoleWebClient(WebClient.Builder webClientBuilder, String roleEndpoint) {
        this.webClientBuilder = webClientBuilder;
        this.roleEndpoint = roleEndpoint;
    }

    /**
     * this will retrieve roles created by this user-id
     * @param userId
     * @param pageable
     * @return
     */
    public Mono<RestPage<Role>> getRolesByUserId(String accessToken, UUID userId, Pageable pageable) {
        LOG.info("get roles for this ownerId: {}", userId);

        final StringBuilder stringBuilder = new StringBuilder(roleEndpoint);
        stringBuilder.append("/user-id/").append(userId)
                .append("?page=").append(pageable.getPageNumber())
                .append("&size=").append(pageable.getPageSize())
                .append("&sortBy=name");

        LOG.info("get roles by owner using userId at endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken)).retrieve();
        return responseSpec.bodyToMono(new ParameterizedTypeReference<RestPage<Role>>() {});
    }

    /**
     * get associated roles for organization-id
     * @param organizationId
     * @param pageable
     * @return
     */
    public Mono<RestPage<Role>> getRolesByOrganizationId(String accessToken, UUID organizationId, Pageable pageable) {
        LOG.info("get roles for this organizationId: {}", organizationId);

        final StringBuilder stringBuilder = new StringBuilder(roleEndpoint);
        stringBuilder.append("/organizations/").append(organizationId)
                .append("?page=").append(pageable.getPageNumber())
                .append("&size=").append(pageable.getPageSize())
                .append("&sortBy=name");
        LOG.info("get roles for organization at endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken)).retrieve();
        return responseSpec.bodyToMono(new ParameterizedTypeReference<RestPage<Role>>() {}).doOnNext(roleCustomRestPage -> {
            if (roleCustomRestPage != null) {
                LOG.info("got roles: {}", roleCustomRestPage.content());
            }
        });
    }

    // use httpMethod for update or post
    public Mono<Role> updateRole(String accessToken, Role role, HttpMethod httpMethod) {
        LOG.info("update role with endpoint: {}", roleEndpoint);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().method(httpMethod).uri(roleEndpoint)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .bodyValue(role)
                .retrieve();
        return responseSpec.bodyToMono(Role.class).flatMap(role1-> {
            LOG.info("saved role");
            return Mono.just(role1);
        });
    }

    public Mono<String> deleteRole(String accessToken, UUID id) {
        LOG.info("delete role by id: {}", id);
        final StringBuilder stringBuilder = new StringBuilder(roleEndpoint);
        stringBuilder.append("/").append(id);
        LOG.info("delete role endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().delete().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken)).retrieve();

        return responseSpec.bodyToMono(String.class).flatMap(string -> {
            LOG.info("role deleted");
            return Mono.just(string);
        });
    }

    public Mono<Role> getRoleById(String accessToken, UUID id) {
        LOG.info("get role by id: {}", id);
        final StringBuilder stringBuilder = new StringBuilder(roleEndpoint);
        stringBuilder.append("/").append(id);
        LOG.info("get role by id endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken)).retrieve();

        return responseSpec.bodyToMono(Role.class);
    }

    /**
     * Get User roles from role-rest-service that has this organizationId, and clientId and matching UserIds
     *  class ClientOrgUsersWithRole {
     *      clientId,
     *      orgId,
     *      User: {userId, Role}
     *  }
     * @param clientId
     * @param organizationId
     * @param userIds
     * @return
     */
    public Mono<List<ClientOrganizationUserWithRole>> getClientOrganizationUserWithRoles(String accessToken, UUID clientId, UUID organizationId, List<UUID> userIds) {
        LOG.info("get an object that has the clientId, organizationId, a list of UserIds with their roles (id, name)");

        String endpoint = (roleEndpoint + "/clients/{clientId}/organizations/{organizationId}/users/roles")
                .replace("{clientId}", clientId.toString())
                .replace("{organizationId}", organizationId.toString());

        StringBuilder userIdString = new StringBuilder();
        for(int i = 0; i < userIds.size(); i++) {
            userIdString.append(userIds.get(i));
            if (i+1 < userIds.size()) {
                userIdString.append(",");
            }
        }
        LOG.debug("userIdString: {}", userIdString);

        LOG.info("get clientOrganizationUserWithRoles with endpoint: {}", endpoint);
        WebClient.ResponseSpec responseSpec = webClientBuilder.build().put().uri(endpoint)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken)).bodyValue(userIdString).retrieve();

        return responseSpec.bodyToMono(new ParameterizedTypeReference<List<ClientOrganizationUserWithRole>>() {});
    }

    public Mono<ClientOrganizationUserRole> addClientOrganizationUserRole(String accessToken, ClientOrganizationUserWithRole clientOrganizationUserWithRole) {
        LOG.info("add client organization user role: {}", clientOrganizationUserWithRole);

        final StringBuilder stringBuilder = new StringBuilder(roleEndpoint);
        stringBuilder.append("/clients/organizations/users/roles");

        LOG.info("add clientOrganizationUserRoles endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().post().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .bodyValue(clientOrganizationUserWithRole).retrieve();
        return responseSpec.bodyToMono(ClientOrganizationUserRole.class);
    }

    /**
     * this id represents the {@link ClientOrganizationUserRole#getId()}
     * @param id
     * @return
     */
    public Mono<String> deleteClientOrganizationUserRole(String accessToken, UUID id) {
        LOG.info("delete client organization user role by its id: {}", id);

        final StringBuilder stringBuilder = new StringBuilder(roleEndpoint);
        stringBuilder.append("/clients/organizations/users/roles/").append(id);
        String endpoint = stringBuilder.toString();

        LOG.info("delete clientOrganizationUserRole by id endpoint: {}", endpoint);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().delete().uri(endpoint)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken)).retrieve();
        return responseSpec.bodyToMono(String.class);
    }

    public Mono<RoleOrganization> addRoleToOrganization(String accessToken, RoleOrganization roleOrganization) {
        LOG.info("add role to organization");

        final StringBuilder stringBuilder = new StringBuilder(roleEndpoint);
        stringBuilder.append("/organizations");


        LOG.info("add role to organization endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().post().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .bodyValue(roleOrganization).retrieve();
        return responseSpec.bodyToMono(RoleOrganization.class);
    }

    public Mono<String> deleteRoleOrganization(String accessToken, UUID roleId, UUID organizationId) {
        LOG.info("delete roleOrganization by id");

        final StringBuilder stringBuilder = new StringBuilder(roleEndpoint).append("/").append(roleId);
        stringBuilder.append("/organizations/").append(organizationId);

        LOG.info("delete roleOrganization ndpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().delete().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken)).retrieve();
        return responseSpec.bodyToMono(String.class);
    }
    public Mono<Map<String, String>> getAuthzManagerRoleByName(String accessToken, String name) {
        LOG.info("get AuthzManagerRole id by name {}", name);

        final StringBuilder stringBuilder = new StringBuilder(roleEndpoint);
        stringBuilder.append("/authzmanagerroles/name");
        LOG.info("get authzManagerRoleId by endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().put().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .bodyValue(name).retrieve();
        return responseSpec.bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {});
    }

    //   return responseSpec.bodyToMono(new ParameterizedTypeReference<CustomRestPage<Role>>() {});

    public Mono<RestPage<UUID>> getOrgIdsOfSuperAdminOrganizationForUser(String accessToken, Pageable pageable) {
        final StringBuilder stringBuilder = new StringBuilder(roleEndpoint);
        stringBuilder.append("/authzmanagerroles/users/organizations")
             .append("?page=").append(pageable.getPageNumber()).append("&size=").append(pageable.getPageSize());
        LOG.info("get orgIds for super-admin organizations for logged-in userId using endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken)).retrieve();
        return responseSpec.bodyToMono(new ParameterizedTypeReference<RestPage<UUID>>() {})
                .doOnNext(uuidPage -> LOG.info("got response: {}", uuidPage));
    }

    public Mono<Map<UUID, UUID>> areUsersSuperAdminInDefaultOrgId(String accessToken, UUID organizationId, List<UUID> userIdList) {
        LOG.info("check if user {} is superAdmin in the default organizationId {}", userIdList, organizationId);

        final StringBuilder stringBuilder = new StringBuilder(roleEndpoint);
        stringBuilder.append("/authzmanagerroles/users/organizations/").append(organizationId);
        LOG.info("get a list back to find if they are superAdmin using endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().put().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .bodyValue(userIdList).retrieve();
        return responseSpec.bodyToMono(new ParameterizedTypeReference<Map<UUID, UUID>>() {});
    }

    public Mono<Map<String, Object>> addUserToSuperAdminRoleInOrganization(String accessToken, UUID authzManagerRoleId, UUID organizationId, UUID targetUserId, Pageable pageable) {
        LOG.info("add user {} to superAdmin role to organization id {}", targetUserId, organizationId);

        final StringBuilder stringBuilder = new StringBuilder(roleEndpoint);
        stringBuilder.append("/authzmanagerroles/users/organizations").append("?page=").append(pageable.getPageNumber())
                .append("&size=").append(pageable.getPageSize());
        LOG.info("endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().post().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .bodyValue(Map.of("userId", targetUserId, "organizationId", organizationId, "authzManagerRoleId", authzManagerRoleId)).retrieve();
        return responseSpec.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    public Mono<String> deleteUserFromAuthzManagerRoleOrganization(String accessToken, UUID authzManagerRoleOrganizationId) {
        LOG.info("deleteUserFromAuthzManagerRoleOrganization by id {}", authzManagerRoleOrganizationId);

        final StringBuilder stringBuilder = new StringBuilder(roleEndpoint).append("/");
        stringBuilder.append("/authzmanagerroles/users/organizations/").append(authzManagerRoleOrganizationId);

        LOG.info("deleteUserFromAuthzManagerRoleOrganization endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().delete().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken)).retrieve();
        return responseSpec.bodyToMono(String.class);
    }

    public Mono<Boolean> isSuperAdminInOrgId(String accessToken, UUID userId, UUID organizationId) {
        LOG.info("get superAdmin organizations count for this user in accessToken");

        final StringBuilder stringBuilder = new StringBuilder(roleEndpoint);
        stringBuilder.append("/authzmanagerroles/users/").append(userId).append("/organizations/").append(organizationId);
        LOG.info("is user superAdmin in orgId using endpoint: {}", stringBuilder);

        WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec = webClientBuilder.build().get();

        if (accessToken != null) {
            requestHeadersUriSpec.headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken));
        }

        return requestHeadersUriSpec.uri(stringBuilder.toString())
                .retrieve().bodyToMono(new ParameterizedTypeReference<Map<String, Boolean>>() {})
                .flatMap(map -> {
                    LOG.info("response for is user a super admin in orgId: {}", map);
                    if (map.get("message") != null) {
                        return Mono.just(map.get("message"));
                    }
                    else {
                        return Mono.error(new BadRequestException("There is no message in the response"));
                    }
                }).onErrorResume(throwable -> {
                    LOG.error("error occurred when checking if user is super admin for orgId", throwable);
                    return Mono.error(throwable);
                });
    }
}
