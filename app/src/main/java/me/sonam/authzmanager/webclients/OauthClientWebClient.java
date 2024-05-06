package me.sonam.authzmanager.webclients;

import me.sonam.authzmanager.controller.admin.oauth2.RegisteredClient;
import me.sonam.authzmanager.controller.admin.oauth2.util.RegisteredClientUtil;
import me.sonam.authzmanager.controller.util.MyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.*;

public class OauthClientWebClient/* implements OauthClientRoute*/ {
    private static final Logger LOG = LoggerFactory.getLogger(OauthClientWebClient.class);

    private final String clientsEndpoint;

    private final WebClient.Builder webClientBuilder;
    private RegisteredClientUtil registeredClientUtil = new RegisteredClientUtil();

    public OauthClientWebClient(WebClient.Builder webclientBuilder, String clientsEndpoint) {
        this.webClientBuilder = webclientBuilder;
        this.clientsEndpoint = clientsEndpoint;
    }
    
    public Mono<RegisteredClient> createClient(Map<String, Object> map) {
        LOG.info("calling auth-server create client endpoint {}", clientsEndpoint);

        LOG.info("payload: {}", map);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().post().uri(clientsEndpoint)
                .bodyValue(map).retrieve();
        return responseSpec.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>(){}).map(responseMap-> {
            LOG.info("got back response from auth-server create client call: {}", responseMap);
            return registeredClientUtil.build(responseMap);
        }).onErrorResume(throwable -> {
            String stringBuilder = "auth-server create client failed: " +
                    throwable.getMessage();

            if (throwable instanceof WebClientResponseException) {
                WebClientResponseException webClientResponseException = (WebClientResponseException) throwable;
                LOG.error("error body contains: {}", webClientResponseException.getResponseBodyAsString());
            }
            else {
                LOG.error(stringBuilder, throwable);
            }

            return Mono.error(throwable);
        });
    }

    
    public Mono<RegisteredClient> updateClient(Map<String, Object> map, HttpMethod httpMethod) {
        LOG.info("update client with endpoint {}", clientsEndpoint);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().method(httpMethod).uri(clientsEndpoint)
                .bodyValue(map).retrieve();
        return responseSpec.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>(){}).map(responseMap-> {
            LOG.info("got back response from auth-server update client call: {}", responseMap);

            return registeredClientUtil.build(responseMap);
        }).onErrorResume(throwable -> {
            String stringBuilder = "auth-server update client failed: " +
                    throwable.getMessage();
            LOG.error(stringBuilder, throwable);
            return Mono.error(throwable);
        });
    }

    
    public Mono<Void> deleteClient(UUID id, UUID userId) {
        LOG.info("delete client by id: {} and ownerId: {}", id, userId);

        StringBuilder deleteEndpoint = new StringBuilder(clientsEndpoint)
                .append(id).append("/user-id/").append(userId);
        LOG.info("calling auth-server delete client endpoint {}", deleteEndpoint);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().delete().uri(deleteEndpoint.toString())
                .retrieve();

        return responseSpec.bodyToMono(String.class).then();
    }

    /**
     * this will call authorization-server clients endpoints to get
     * clientIds for a given userId <a href="http://authorization-server/clients/">...</a>{userId}
     *
     * @param userId user id of user
     * @return return a list of clientId strings
     */
    
    public Mono<List<MyPair<String, String>>> getUserClientIds(UUID userId) {
        LOG.info("get user '{}' clients", userId);

        StringBuilder clientsEndpoint = new StringBuilder(this.clientsEndpoint).append("/users/")
                .append(userId.toString());
        LOG.info("calling auth-server get clientIds for userId endpoint {}", clientsEndpoint);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(clientsEndpoint.toString())
                .retrieve();

        return responseSpec.bodyToMono(new ParameterizedTypeReference<List<MyPair<String, String>>>() {}).map(list-> {
            LOG.info("got back response from auth-server for List of Pairs with id and client name call: {}", list);
            return list;
        }).onErrorResume(throwable -> {
            String errorMessage = "auth-server get clientIds for userId failed: " +
                    throwable.getMessage();
            LOG.error(errorMessage);
            return Mono.error(throwable);
        });
    }

    
    public Mono<RegisteredClient> getOauthClientByClientId(String clientId) {
        StringBuilder clientsEndpoint = new StringBuilder(this.clientsEndpoint).append("/client-id/").append(clientId);
        LOG.info("calling auth-server get clientId by clientId with endpoint {}", clientsEndpoint);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(clientsEndpoint.toString())
                .retrieve();
        return responseSpec.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {}).map(map-> {
            LOG.info("got back response from auth-server get clientId by clientId  call: {}", map);

            return registeredClientUtil.build(map);
        }).onErrorResume(throwable -> {
            String errorMessage = "auth-server get clientId by clientId failed: " +
                    throwable.getMessage();
            LOG.error(errorMessage);
            return Mono.error(throwable);
        });
    }

    
    public Mono<RegisteredClient> getOauthClientById(UUID id) {
        StringBuilder clientsEndpoint = new StringBuilder(this.clientsEndpoint).append(id);
        LOG.info("calling auth-server get clientId by clientId with endpoint {}", clientsEndpoint);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(clientsEndpoint.toString())
                .retrieve();
        return responseSpec.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {}).map(map-> {
            LOG.info("got back response from auth-server get clientId by clientId  call: {}", map);

            return registeredClientUtil.build(map);
        }).onErrorResume(throwable -> {
            String errorMessage = "auth-server get clientId by clientId failed: " +
                    throwable.getMessage();
            LOG.error(errorMessage);
            return Mono.error(throwable);
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