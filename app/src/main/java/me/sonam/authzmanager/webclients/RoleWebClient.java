package me.sonam.authzmanager.webclients;



import me.sonam.authzmanager.clients.user.ClientOrganizationUserRole;
import me.sonam.authzmanager.controller.clients.carrier.ClientOrganizationUserWithRole;
import me.sonam.authzmanager.controller.admin.roles.Role;
import me.sonam.authzmanager.rest.RestPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public class RoleWebClient {
    private static final Logger LOG = LoggerFactory.getLogger(RoleWebClient.class);

    private WebClient.Builder webClientBuilder;
    private String roleEndpoint;

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
    public Mono<RestPage<Role>> getRolesByUserId(UUID userId, Pageable pageable) {
        LOG.info("get roles for this ownerId: {}", userId);

        final StringBuilder stringBuilder = new StringBuilder(roleEndpoint);
        stringBuilder.append("/user-id/").append(userId)
          .append("?page=").append(pageable.getPageNumber())
                .append("&size=").append(pageable.getPageSize())
                .append("&sortBy=name");

        LOG.info("get roles by owner using userId at endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(stringBuilder.toString())
                .retrieve();
        return responseSpec.bodyToMono(new ParameterizedTypeReference<RestPage<Role>>() {});
    }

    /**
     * get associated roles for organization-id
     * @param organizationId
     * @param pageable
     * @return
     */
    public Mono<RestPage<Role>> getRolesByOrganizationId(UUID organizationId, Pageable pageable) {
        LOG.info("get roles for this organizationId: {}", organizationId);

        final StringBuilder stringBuilder = new StringBuilder(roleEndpoint);
        stringBuilder.append("/organizations/").append(organizationId)
                .append("?page=").append(pageable.getPageNumber())
                .append("&size=").append(pageable.getPageSize())
                .append("&sortBy=r.name");
        LOG.info("get roles for organization at endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(stringBuilder.toString())
                .retrieve();
        return responseSpec.bodyToMono(new ParameterizedTypeReference<RestPage<Role>>() {});
    }

    // use httpMethod for update or post
    public Mono<Role> updateRole(Role role, HttpMethod httpMethod) {
        LOG.info("update role: {}", role);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().method(httpMethod).uri(roleEndpoint)
                .bodyValue(role)
                .retrieve();
        return responseSpec.bodyToMono(Role.class).flatMap(role1-> {
            LOG.info("saved role");
            return Mono.just(role1);
        });
    }

    public Mono<String> deleteRole(UUID id) {
        LOG.info("delete role by id: {}", id);
        final StringBuilder stringBuilder = new StringBuilder(roleEndpoint);
        stringBuilder.append("/").append(id);
        LOG.info("delete role endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().delete().uri(stringBuilder.toString()).
                retrieve();

        return responseSpec.bodyToMono(String.class).flatMap(string -> {
            LOG.info("role deleted");
            return Mono.just(string);
        });
    }

    public Mono<Role> getRoleById(UUID id) {
        LOG.info("get role by id: {}", id);
        final StringBuilder stringBuilder = new StringBuilder(roleEndpoint);
        stringBuilder.append("/").append(id);
        LOG.info("get role by idendpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(stringBuilder.toString()).
                retrieve();

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
    public Mono<List<ClientOrganizationUserWithRole>> getClientOrganizationUserWithRoles(UUID clientId, UUID organizationId, List<UUID> userIds) {
        LOG.info("get an object that has the clientId, organizationId, a list of UserIds with their roles (id, name)");

        String endpoint = (roleEndpoint + "/client-organization-users/client-id/{clientId}/organization-id/{organizationId}/user-ids/{userIds}")
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
        endpoint = endpoint.replace("{userIds}", userIdString);

        LOG.info("get clientOrganizationUserWithRoles with endpoint: {}", endpoint);
        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(endpoint).retrieve();

        return responseSpec.bodyToMono(new ParameterizedTypeReference<List<ClientOrganizationUserWithRole>>() {});
    }

    public Mono<ClientOrganizationUserRole> addClientOrganizationUserRole(ClientOrganizationUserWithRole clientOrganizationUserWithRole) {
        LOG.info("add client organization user role: {}", clientOrganizationUserWithRole);

        final StringBuilder stringBuilder = new StringBuilder(roleEndpoint);
        stringBuilder.append("/client-organization-users");

        LOG.info("add client-organization-user-roles endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().post().uri(stringBuilder.toString())
                .bodyValue(clientOrganizationUserWithRole).retrieve();
        return responseSpec.bodyToMono(ClientOrganizationUserRole.class);
    }

    /**
     * this id represents the {@link ClientOrganizationUserRole#getId()}
     * @param id
     * @return
     */
    public Mono<String> deleteClientOrganizationUserRole(UUID id) {
        LOG.info("delete client organization user role by its id: {}", id);

        final StringBuilder stringBuilder = new StringBuilder(roleEndpoint);
        stringBuilder.append("/client-organization-users/{id}");
        String endpoint = stringBuilder.toString().replace("{id}", id.toString());

        LOG.info("delete client-organization-user-roles endpoint: {}", endpoint);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().delete().uri(endpoint).retrieve();
        return responseSpec.bodyToMono(String.class);
    }
}