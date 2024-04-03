package me.sonam.authzmanager.clients;

import me.sonam.authzmanager.controller.admin.organization.Organization;
import me.sonam.authzmanager.controller.admin.organization.OrganizationController;
import me.sonam.authzmanager.user.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

public class OrganizationWebClient {
    private static final Logger LOG = LoggerFactory.getLogger(OrganizationController.class);

    private WebClient.Builder webClientBuilder;
    private String organizationEndpoint;
    public OrganizationWebClient(WebClient.Builder webClientBuilder, String organizationEndpoint) {
        this.webClientBuilder = webClientBuilder;
        this.organizationEndpoint = organizationEndpoint;
    }

    public Mono<Map<String, String>> getMyOrganizations() {
        LOG.info("get organizations created/owned by this user");

        LOG.info("principal: {}", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        UserId userId = (UserId) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        //LOG.info("requestBody: {}", requestBody);
        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(organizationEndpoint)
                .retrieve();
        return responseSpec.bodyToMono(new ParameterizedTypeReference<Map<String, String>>(){}).map(responseMap-> {
            LOG.info("got back response {}", responseMap);

            return responseMap;
        }).onErrorResume(throwable -> {
            String stringBuilder = "get organizations call failed: " +
                    throwable.getMessage();
            LOG.error(stringBuilder, throwable);
            return Mono.just(Map.of("error", stringBuilder));
        });
    }

    public Mono<UUID> createOrganization(Organization organization) {
        LOG.info("create organization");

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().post().uri(organizationEndpoint)
                .bodyValue(organization)
                .retrieve();
        return responseSpec.bodyToMono(Map.class).map(map-> {
            LOG.info("created organization id {}", map.get("id"));

            return UUID.fromString(map.get("id").toString());
        });
    }
}
