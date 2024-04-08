package me.sonam.authzmanager.clients;



import me.sonam.authzmanager.controller.admin.roles.Role;
import me.sonam.authzmanager.rest.RestPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class RoleWebClient {
    private static final Logger LOG = LoggerFactory.getLogger(RoleWebClient.class);

    private WebClient.Builder webClientBuilder;
    private String roleEndpoint;
    public RoleWebClient(WebClient.Builder webClientBuilder, String roleEndpoint) {
        this.webClientBuilder = webClientBuilder;
        this.roleEndpoint = roleEndpoint;
    }
    public Mono<RestPage<Role>> getRolesByUserId(UUID userId) {
        LOG.info("get roles for this ownerId: {}", userId);

        final StringBuilder stringBuilder = new StringBuilder(roleEndpoint);
        stringBuilder.append("/userId/").append(userId);
        LOG.info("get roles by owner using userId at endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(stringBuilder.toString())
                .retrieve();
        return responseSpec.bodyToMono(new ParameterizedTypeReference<RestPage<Role>>() {});
    }

    public Mono<RestPage<Role>> getRoles(UUID organizationId) {
        LOG.info("get roles for this organizationId: {}", organizationId);

        final StringBuilder stringBuilder = new StringBuilder(roleEndpoint);
        stringBuilder.append("/organizations/").append(organizationId);
        LOG.info("get roles for organization at endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(stringBuilder.toString())
                .retrieve();
        return responseSpec.bodyToMono(new ParameterizedTypeReference<RestPage<Role>>() {});
    }

    // use httpMethod for update or post

    public Mono<Role> updateRole(Role role, HttpMethod httpMethod) {
        LOG.info("create role: {}", role);

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
}
