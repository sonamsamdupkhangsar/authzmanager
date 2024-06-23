package me.sonam.authzmanager.webclients;


import me.sonam.authzmanager.AuthzManagerException;
import me.sonam.authzmanager.controller.admin.organization.Organization;

import me.sonam.authzmanager.rest.RestPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

public class OrganizationWebClient {
    private static final Logger LOG = LoggerFactory.getLogger(OrganizationWebClient.class);

    private final WebClient.Builder webClientBuilder;
    private final String organizationEndpoint;

    public OrganizationWebClient(WebClient.Builder webClientBuilder, String organizationEndpoint) {
        this.webClientBuilder = webClientBuilder;
        this.organizationEndpoint = organizationEndpoint;
    }

    public Mono<RestPage<Organization>> getOrganizationPageByOwner(String accessToken, UUID userId, Pageable pageable) {
        LOG.info("get organizations created/owned by this user");

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            LOG.error("authentication is null");
        }
        else {
            LOG.info("authentication is not null {}", authentication);
        }
        final StringBuilder stringBuilder = new StringBuilder(organizationEndpoint);
        stringBuilder.append("/owner/").append(userId)
                .append("?page=").append(pageable.getPageNumber())
                .append("&size=").append(pageable.getPageSize())
                .append("&sortBy=name");
        LOG.info("get organizations for user at endpoint: {}", stringBuilder);
        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .retrieve();
        return responseSpec.bodyToMono(new ParameterizedTypeReference<RestPage<Organization>>() {});
    }

    // use httpMethod for update or post

    public Mono<Organization> updateOrganization(String accessToken, Organization organization, HttpMethod httpMethod) {
        LOG.info("create organization: {} with endpoint: {}", organization, organizationEndpoint);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().method(httpMethod).uri(organizationEndpoint)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .bodyValue(organization)
                .retrieve();
        return responseSpec.bodyToMono(Organization.class).flatMap(organization1-> {
            LOG.info("saved organization");
            return Mono.just(organization1);
        });
    }

    public Mono<String> deleteOrganization(String accessToken, UUID organizationId) {
        LOG.info("delete organization by id: {}", organizationId);
        final StringBuilder stringBuilder = new StringBuilder(organizationEndpoint);
        stringBuilder.append("/").append(organizationId);
        LOG.info("delete organization endpoint: {}", stringBuilder.toString());

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().delete().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .retrieve();

        return responseSpec.bodyToMono(String.class).flatMap(string -> {
            LOG.info("organization deleted");
            return Mono.just(string);
        });
    }

    public Mono<Organization> getOrganizationById(String accessToken, UUID id) {
        LOG.info("get organization by id: {}", id);
        final StringBuilder stringBuilder = new StringBuilder(organizationEndpoint);
        stringBuilder.append("/").append(id);
        LOG.info("get organization by id endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .retrieve();

        return responseSpec.bodyToMono(Organization.class).onErrorResume(throwable -> {
            LOG.error("error occured on getting organization by id", throwable);
            return Mono.empty();
        });
    }

    public Mono<RestPage<UUID>> getUsersInOrganizationId(String accessToken, UUID id, Pageable pageable) {
        LOG.info("get users by organization id: {}", id);
        final StringBuilder stringBuilder = new StringBuilder(organizationEndpoint);
        stringBuilder.append("/").append(id).append("/users")
                .append("?page=").append(pageable.getPageNumber())
                .append("&size=").append(pageable.getPageSize());

        LOG.info("get users in organization by id endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken)).retrieve();

        return responseSpec.bodyToMono(new ParameterizedTypeReference<RestPage<UUID>>() {})
                .onErrorResume(throwable -> {
                    LOG.error("error retrieving users in organization by id", throwable);
                    return Mono.error(throwable);
                });
    }

    public Mono<Boolean> userExistsInOrganization(String accessToken, UUID userId, UUID organizationId) {
        LOG.info("check if user {} exists in organization {}", userId, organizationId);
        final StringBuilder stringBuilder = new StringBuilder(organizationEndpoint);
        stringBuilder.append("/").append(organizationId).append("/users/").append(userId);

        LOG.info("checking user exists in organization by id endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken)).retrieve();

        return responseSpec.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(map -> Boolean.parseBoolean(map.get("message").toString()))
                .onErrorResume(throwable -> {
                    LOG.error("returning false on error to call user exists in organization call", throwable);
                    return Mono.just(false);
                });
    }

    public Mono<Map<String, String>> addUserToOrganization(String accessToken, UUID userId, UUID organizationId) {
        LOG.info("add user {} to organization {}", userId, organizationId);

        final StringBuilder stringBuilder = new StringBuilder(organizationEndpoint);
        stringBuilder.append("/users");

        LOG.info("add user to organization endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().post().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .bodyValue(Map.of("userId", userId, "organizationId", organizationId)).retrieve();

        return responseSpec.bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {});
    }

    public Mono<Map<String, String>> removeUserFromOrganization(String accessToken, UUID userId, UUID organizationId) {
        LOG.info("remove user {} from organization {}", userId, organizationId);

        final StringBuilder stringBuilder = new StringBuilder(organizationEndpoint);
        stringBuilder.append("/").append(organizationId).append("/users/").append(userId);

        LOG.info("remove user from organization endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().delete().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken)).retrieve();

        return responseSpec.bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {});
    }
}
