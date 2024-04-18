package me.sonam.authzmanager.clients;


import me.sonam.authzmanager.clients.user.UserWebClient;
import me.sonam.authzmanager.controller.admin.organization.Organization;
import me.sonam.authzmanager.controller.admin.organization.OrganizationController;

import me.sonam.authzmanager.rest.RestPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

public class OrganizationWebClient {
    private static final Logger LOG = LoggerFactory.getLogger(OrganizationController.class);

    private final WebClient.Builder webClientBuilder;
    private final String organizationEndpoint;

    public OrganizationWebClient(WebClient.Builder webClientBuilder, String organizationEndpoint) {
        this.webClientBuilder = webClientBuilder;
        this.organizationEndpoint = organizationEndpoint;
    }

    public Mono<RestPage<Organization>> getMyOrganizations(UUID userId, Pageable pageable) {
        LOG.info("get organizations created/owned by this user");

        final StringBuilder stringBuilder = new StringBuilder(organizationEndpoint);
        stringBuilder.append("/owner/").append(userId)
                .append("?page=").append(pageable.getPageNumber())
                .append("&size=").append(pageable.getPageSize())
                .append("&sortBy=name");
        LOG.info("get organizations for user at endpoint: {}", stringBuilder);
        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(stringBuilder.toString())
                .retrieve();
        return responseSpec.bodyToMono(new ParameterizedTypeReference<RestPage<Organization>>() {});
    }

    // use httpMethod for update or post

    public Mono<Organization> updateOrganization(Organization organization, HttpMethod httpMethod) {
        LOG.info("create organization: {}", organization);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().method(httpMethod).uri(organizationEndpoint)
                .bodyValue(organization)
                .retrieve();
        return responseSpec.bodyToMono(Organization.class).flatMap(organization1-> {
            LOG.info("saved organization");
            return Mono.just(organization1);
        });
    }

    public Mono<String> deleteOrganization(UUID organizationId) {
        LOG.info("delete organization by id: {}", organizationId);
        final StringBuilder stringBuilder = new StringBuilder(organizationEndpoint);
        stringBuilder.append("/").append(organizationId);
        LOG.info("delete organization endpoint: {}", stringBuilder.toString());

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().delete().uri(stringBuilder.toString()).
                retrieve();

        return responseSpec.bodyToMono(String.class).flatMap(string -> {
            LOG.info("organization deleted");
            return Mono.just(string);
        });
    }

    public Mono<Organization> getOrganizationById(UUID id) {
        LOG.info("get organization by id: {}", id);
        final StringBuilder stringBuilder = new StringBuilder(organizationEndpoint);
        stringBuilder.append("/").append(id);
        LOG.info("get organization by idendpoint: {}", stringBuilder.toString());

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(stringBuilder.toString()).
                retrieve();

        return responseSpec.bodyToMono(Organization.class);
    }

    public Mono<RestPage<UUID>> getUsersInOrganizationId(UUID id, Pageable pageable) {
        LOG.info("get users by organization id: {}", id);
        final StringBuilder stringBuilder = new StringBuilder(organizationEndpoint);
        stringBuilder.append("/").append(id).append("/users")
                .append("?page=").append(pageable.getPageNumber())
                .append("&size=").append(pageable.getPageSize());

        LOG.info("get users in organization by id endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(stringBuilder.toString()).
                retrieve();

        return responseSpec.bodyToMono(new ParameterizedTypeReference<RestPage<UUID>>() {});
    }

    public Mono<Boolean> userExistsInOrganization(UUID userId, UUID organizationId) {
        LOG.info("check if user {} exists in organization {}", userId, organizationId);
        final StringBuilder stringBuilder = new StringBuilder(organizationEndpoint);
        stringBuilder.append("/").append(organizationId).append("/users/").append(userId);

        LOG.info("checking user exists in organization by id endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(stringBuilder.toString()).
                retrieve();

        return responseSpec.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(map -> Boolean.parseBoolean(map.get("message").toString()))
                .onErrorResume(throwable -> {
                    LOG.error("returning false on error to call user exists in organization call", throwable);
                    return Mono.just(false);
                });
    }

    public Mono<Map<String, String>> addUserToOrganization(UUID userId, UUID organizationId) {
        LOG.info("add user {} to organization {}", userId, organizationId);

        final StringBuilder stringBuilder = new StringBuilder(organizationEndpoint);
        stringBuilder.append("/users");

        LOG.info("add user to organization endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().post().uri(stringBuilder.toString())
            .bodyValue(Map.of("userId", userId, "organizationId", organizationId)).retrieve();

        return responseSpec.bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {});
    }

    public Mono<Map<String, String>> removeUserFromOrganization(UUID userId, UUID organizationId) {
        LOG.info("add user {} to organization {}", userId, organizationId);

        final StringBuilder stringBuilder = new StringBuilder(organizationEndpoint);
        stringBuilder.append("/id/").append(organizationId).append("/users/userId/").append(userId);

        LOG.info("add user to organization endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().delete().uri(stringBuilder.toString())
                .retrieve();

        return responseSpec.bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {});
    }
}
