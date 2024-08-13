package me.sonam.authzmanager.webclients;

import me.sonam.authzmanager.AuthzManagerException;
import me.sonam.authzmanager.clients.user.ClientOrganization;
import me.sonam.authzmanager.controller.admin.organization.Organization;
import me.sonam.authzmanager.controller.util.MyPair;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClientOrganizationWebClient {
    private static final Logger LOG = LoggerFactory.getLogger(ClientOrganizationWebClient.class);
    private final WebClient.Builder webClientBuilder;
    private final String clientOrganizationEndpoint;

    public ClientOrganizationWebClient(WebClient.Builder webclientBuilder, String clientOrganizationEndpoint) {
        this.webClientBuilder = webclientBuilder;
        this.clientOrganizationEndpoint = clientOrganizationEndpoint;
    }

    public Mono<String> addClientToOrganization(String accessToken, UUID clientsId, UUID organizationId) {
        LOG.info("add client {} to organization: {} with endpoint: {}", clientsId, organizationId, clientOrganizationEndpoint);

        String clientOrganizations = new StringBuilder(clientOrganizationEndpoint).append("/organizations").toString();

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().post().uri(clientOrganizations)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .bodyValue(new ClientOrganization(clientsId, organizationId))
                .retrieve();
        return responseSpec.bodyToMono(String.class).map(string-> {
            LOG.info("got back response from auth-server get clientId by clientId  call: {}", string);

            return string;
        }).onErrorResume(throwable -> {
            final String serviceErrorMessage = getErrorMessage(throwable);
            LOG.info("serviceErrorMessage: {}", serviceErrorMessage);
            return Mono.just(serviceErrorMessage);
        });
    }

    public Mono<String> deleteClientOrganizationAssociation(String accessToken, UUID clientsId, UUID organizationId) {
        LOG.info("delete clientOrganization association: clientId: {}, organizationId: {}",
                clientsId, organizationId);

        StringBuilder clientsEndpoint = new StringBuilder(this.clientOrganizationEndpoint).append("/")
                .append(clientsId).append("/organizations/").append(organizationId);
        LOG.info("calling auth-server get clientId by clientId with endpoint {}", clientsEndpoint);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().delete().uri(clientsEndpoint.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .retrieve();
        return responseSpec.bodyToMono(String.class).map(string-> {
            LOG.info("got back response from auth-server delete client and organization id call: {}", string);

            return string;
        }).onErrorResume(throwable -> {
            final String serviceErrorMessage = getErrorMessage(throwable);
            LOG.info("serviceErrorMessage: {}", serviceErrorMessage);
            return Mono.just(serviceErrorMessage);
        });
    }

    public Mono<ClientOrganization> getClientIdOrganizationIdMatch(String accessToken, List<Organization> organizationList, UUID clientsId) {
        StringBuilder clientsEndpoint = new StringBuilder(this.clientOrganizationEndpoint).append("/organizations");
        LOG.info("calling auth-server find row with clientsId and organizationList endpoint {}", clientsEndpoint);
        List<UUID> organizationIds = organizationList.stream().map(Organization::getId).toList();

        MyPair<UUID, List<UUID>> myPair = new MyPair<>(clientsId, organizationIds);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().put().uri(clientsEndpoint.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .bodyValue(myPair).retrieve();

        return responseSpec.bodyToMono(ClientOrganization.class).map(clientOrganization-> {
            LOG.info("got back response from auth-server for clientId and organizationIds: {}", clientOrganization);

            return clientOrganization;
        }).onErrorResume(throwable -> {
            final String serviceErrorMessage = getErrorMessage(throwable);
            LOG.info("serviceErrorMessage: {}", serviceErrorMessage);
            return Mono.empty();
        });
    }

    public Mono<UUID> getOrganizationIdAssociatedWithClientId(String accessToken, UUID id) {
        LOG.info("calling ClientOrganization endpoint to get organizationId from client.id: {}", id);

        StringBuilder clientsEndpoint = new StringBuilder(this.clientOrganizationEndpoint).append("/")
                .append(id).append("/organizations/id");
        LOG.info("calling auth-server get organizationId endpoint {}", clientsEndpoint);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(clientsEndpoint.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken)).retrieve();

        return responseSpec.bodyToMono(UUID.class)
                        .switchIfEmpty(Mono.error(
                                new AuthzManagerException("Please select a organization for this client.")))
                .map(uuid-> {
                    LOG.info("got back response from auth-server for getting organizationId for client.id: {}", uuid);

                    return uuid;
                }).onErrorResume(throwable -> {
                    LOG.error("error occured: {}", throwable.getMessage());
                    return Mono.error(new AuthzManagerException("No Organization selected"));
                    //return Mono.empty();
                });
    }

    private String getErrorMessage(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException webClientResponseException = (WebClientResponseException) throwable;
            LOG.error("error body contains: {}", webClientResponseException.getResponseBodyAsString());


            if (webClientResponseException.getResponseBodyAsString().contains("\"error\":")) {
                Map<String, String> errorResponseMap = webClientResponseException.getResponseBodyAs(new ParameterizedTypeReference<Map<String, String>>() {
                });
                if (errorResponseMap != null) {
                    LOG.info("map contains error key");
                    return errorResponseMap.get("error");
                } else {
                    return webClientResponseException.getResponseBodyAsString();
                }
            }
        }
        LOG.info("return the throwable message");
        return "error message: " + throwable.getMessage();
    }
}
