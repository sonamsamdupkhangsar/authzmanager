package me.sonam.authzmanager.webclients;


import me.sonam.authzmanager.AuthzManagerException;
import me.sonam.authzmanager.controller.admin.organization.Organization;
import me.sonam.authzmanager.controller.admin.subdomain.Subdomain;
import me.sonam.authzmanager.rest.RestPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
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
        return responseSpec.bodyToMono(
        new ParameterizedTypeReference<RestPage<Organization>>() {}).doOnNext(organizationCustomRestPage -> {
            LOG.info("organizations page by owner {}", organizationCustomRestPage);
        });
                /*.flatMap(string ->{
            LOG.info("response: {}", string);
            ObjectMapper objectMapper = new ObjectMapper();
            CustomRestPage<Organization> customRestPage = null;
            try {
                customRestPage = objectMapper.readValue(string, CustomRestPage.class);
                LOG.info("got customRestPage from string");
            } catch (JsonProcessingException e) {
                LOG.error("json error", e);
                throw new RuntimeException(e);
            }
            return Mono.just(customRestPage);
        });*///new ParameterizedTypeReference<CustomRestPage<Organization>>() {});
    }

    // use httpMethod for update or post
    public Mono<Organization> updateOrganization(String accessToken, Organization organization, HttpMethod httpMethod) {
        LOG.info("update organization: {} with endpoint: {}", organization, organizationEndpoint);

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
            LOG.error("error occurred on getting organization by id", throwable);
            return Mono.empty();
        });
    }

    public Mono<RestPage<UUID>> getUserIdsInOrganizationId(String accessToken, UUID id, Pageable pageable) {
        final StringBuilder stringBuilder = new StringBuilder(organizationEndpoint);
        stringBuilder.append("/").append(id).append("/users")
                .append("?page=").append(pageable.getPageNumber())
                .append("&size=").append(pageable.getPageSize());

        LOG.info("get user ids in organization by id endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken)).retrieve();

        return responseSpec.bodyToMono(new ParameterizedTypeReference<RestPage<UUID>>() {})
                .doOnNext(uuids -> LOG.info("got userIds: {}", uuids))
                .onErrorResume(throwable -> {
                    LOG.error("error retrieving users in organization by id", throwable);
                    return Mono.error(throwable);
                });
    }

    public Mono<Subdomain> getSubdomainByHost(String accessToken, String subdomain) {
        LOG.info("get subdomain by host {}", subdomain);

        final StringBuilder stringBuilder = new StringBuilder(organizationEndpoint);
        stringBuilder.append("/subdomains/").append(subdomain);

        LOG.info("get subdomain by host endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken)).retrieve();

        return responseSpec.bodyToMono(Subdomain.class)
                .onErrorResume(throwable -> {
                    LOG.error("failed to get subdomain by host {}", subdomain, throwable);
                    return Mono.error(throwable);
                });
    }

    public Mono<RestPage<Organization>> getOrganizationsBySubdomain(String accessToken, String subdomain, Pageable pageable) {
        LOG.info("get organizations by subdomain {}", subdomain);

        final StringBuilder stringBuilder = new StringBuilder(organizationEndpoint);
        stringBuilder.append("/subdomain/").append(subdomain).append("/organizations")
                .append("?page=").append(pageable.getPageNumber())
                .append("&size=").append(pageable.getPageSize());

        LOG.info("get organizations by subdomain endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken)).retrieve();

        return responseSpec.bodyToMono(new ParameterizedTypeReference<RestPage<Organization>>() {})
                .onErrorResume(throwable -> {
                    LOG.error("failed to get organizations by subdomain {}", subdomain, throwable);
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

    public Mono<List<UUID>> getOrganizationIdsForUser(String accessToken, UUID userId) {
        LOG.info("get organization ids for user {}", userId);

        final StringBuilder stringBuilder = new StringBuilder(organizationEndpoint);
        stringBuilder.append("/users/").append(userId).append("/ids");

        LOG.info("get organization ids for user endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .retrieve();

        return responseSpec.bodyToMono(new ParameterizedTypeReference<List<UUID>>() {})
                .onErrorResume(throwable -> {
                    LOG.error("failed to get organization ids for user {}", userId, throwable);
                    return Mono.error(throwable);
                });
    }

    public Mono<UUID> getDefaultOrganizationIdForUser(String accessToken, UUID userId, String subdomain) {
        LOG.info("get default organization id for user {} in subdomain {}", userId, subdomain);

        final StringBuilder stringBuilder = new StringBuilder(organizationEndpoint);
        stringBuilder.append("/subdomain/").append(subdomain)
                .append("/users/").append(userId).append("/default-organization-id");

        LOG.info("get default organization id endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .retrieve();

        return responseSpec.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .flatMap(map -> {
                    Object message = map.get("message");
                    if (message == null) {
                        return Mono.empty();
                    }
                    return Mono.just(UUID.fromString(message.toString()));
                })
                .onErrorResume(throwable -> {
                    LOG.error("failed to get default organization id for user {}", userId, throwable);
                    return Mono.error(throwable);
                });
    }

    public Mono<String> setDefaultOrganization(String accessToken, UUID organizationId, UUID userId) {
        LOG.info("set default organization {} for user {}", organizationId, userId);

        final StringBuilder stringBuilder = new StringBuilder(organizationEndpoint);
        stringBuilder.append("/").append(organizationId)
                .append("/users/").append(userId).append("/default");

        LOG.info("set default organization endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().put().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .retrieve();

        return responseSpec.bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {})
                .map(map -> map.get("message"))
                .onErrorResume(throwable -> {
                    LOG.error("failed to set default organization {} for user {}", organizationId, userId, throwable);
                    return Mono.error(throwable);
                });
    }

    public Mono<Map<String, String>> addUserToOrganization(String accessToken, UUID userId, UUID organizationId) {
        return addUserToOrganization(accessToken, userId, organizationId, null, false);
    }

    public Mono<Map<String, String>> addUserToOrganization(String accessToken, UUID userId, UUID organizationId,
                                                           String subdomain, boolean restrictToSubdomain) {
        LOG.info("add user {} to organization {}", userId, organizationId);

        final StringBuilder stringBuilder = new StringBuilder(organizationEndpoint);
        stringBuilder.append("/users");

        LOG.info("add user to organization endpoint: {}", stringBuilder);

        Map<String, Object> body = subdomain == null
                ? Map.of("userId", userId, "organizationId", organizationId)
                : Map.of("userId", userId, "organizationId", organizationId,
                "subdomain", subdomain, "restrictToSubdomain", restrictToSubdomain);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().post().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .bodyValue(body).retrieve();

        return responseSpec.bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {});
    }

    public Mono<Void> organizationBelongsToSubdomain(String accessToken, UUID organizationId, String subdomain) {
        String endpoint = organizationEndpoint + "/subdomain/" + subdomain
                + "/organizations/" + organizationId + "/exists";
        LOG.info("check organization belongs to subdomain endpoint: {}", endpoint);

        return webClientBuilder.build().get().uri(endpoint)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Boolean>>() {})
                .flatMap(response -> {
                    if (Boolean.TRUE.equals(response.get("message"))) {
                        return Mono.empty();
                    }
                    return Mono.error(new AuthzManagerException("organization does not belong to subdomain"));
                });
    }

    public Mono<Map<String, String>> removeUserFromOrganization(String accessToken, UUID userId, UUID organizationId) {
        LOG.info("remove user {} from organization {}", userId, organizationId);

        final StringBuilder stringBuilder = new StringBuilder(organizationEndpoint);
        stringBuilder.append("/").append(organizationId).append("/users/").append(userId);

        LOG.info("remove user from organization endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().delete().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken)).retrieve();

        return responseSpec.bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {})
                .doOnNext(stringStringMap -> LOG.info("got response: {}", stringStringMap));
    }


    public Mono<List<Organization>> getOrganizationByIdsIn(String accessToken, List<UUID> orgIds) {
        final StringBuilder stringBuilder = new StringBuilder(organizationEndpoint);
        stringBuilder.append("/ids");

        LOG.info("get a list of organization by a list of ids using endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().put().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .bodyValue(orgIds).retrieve();

        return responseSpec.bodyToMono(new ParameterizedTypeReference<List<Organization>>() {})
                .doOnNext(organizationList -> LOG.info("got organization by ids response: {}", organizationList));
    }
}
