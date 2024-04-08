package me.sonam.authzmanager.clients;


import me.sonam.authzmanager.controller.admin.organization.Organization;
import me.sonam.authzmanager.controller.admin.organization.OrganizationController;
import me.sonam.authzmanager.rest.RestPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class OrganizationWebClient {
    private static final Logger LOG = LoggerFactory.getLogger(OrganizationController.class);

    private WebClient.Builder webClientBuilder;
    private String organizationEndpoint;
    public OrganizationWebClient(WebClient.Builder webClientBuilder, String organizationEndpoint) {
        this.webClientBuilder = webClientBuilder;
        this.organizationEndpoint = organizationEndpoint;
    }

    public Mono<RestPage<Organization>> getMyOrganizations(UUID userId) {
        LOG.info("get organizations created/owned by this user");

        final StringBuilder stringBuilder = new StringBuilder(organizationEndpoint);
        stringBuilder.append("/owner/").append(userId);
        LOG.info("get organizations for user at endpoint: {}", stringBuilder.toString());
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
}
