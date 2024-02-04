package me.sonam.authzmanager.clients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OauthClientRouteRouteAuthServer implements OauthClientRoute {
    private static final Logger LOG = LoggerFactory.getLogger(OauthClientRouteRouteAuthServer.class);

    private final String clientsEndpoint;

    private final WebClient.Builder webClientBuilder;
    public OauthClientRouteRouteAuthServer(WebClient.Builder webclientBuilder, String clientsEndpoint) {
        this.webClientBuilder = webclientBuilder;
        this.clientsEndpoint = clientsEndpoint;
    }
    @Override
    public Mono<String> createClient(Map<String, String> map) {
        LOG.info("create client");

        LOG.info("calling auth-server create client endpoint {}", clientsEndpoint);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().post().uri(clientsEndpoint)
                .bodyValue(map).retrieve();

        return responseSpec.bodyToMono(String.class).map(string-> {
            LOG.info("got back response from auth-server create client call: {}", string);
            return string;
        }).onErrorResume(throwable -> {
            StringBuilder stringBuilder = new StringBuilder("auth-server create client failed: ")
                    .append(throwable.getMessage());
            LOG.error(stringBuilder.toString());
            return Mono.just(stringBuilder.toString());
        });
    }

    @Override
    public Mono<String> updateClient(Map<String, String> map) {
        LOG.info("update client");

        LOG.info("calling auth-server update client endpoint {}", clientsEndpoint);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().put().uri(clientsEndpoint)
                .bodyValue(map).retrieve();

        return responseSpec.bodyToMono(String.class).map(string-> {
            LOG.info("got back response from auth-server update client call: {}", string);
            return string;
        }).onErrorResume(throwable -> {
            StringBuilder stringBuilder = new StringBuilder("auth-server update client failed: ")
                    .append(throwable.getMessage());
            LOG.error(stringBuilder.toString());
            return Mono.just(stringBuilder.toString());
        });
    }

    @Override
    public Mono<String> deleteClient(String clientId) {
        LOG.info("delete client");

        StringBuilder deleteEndpoint = new StringBuilder(clientsEndpoint).append("/").append(clientId);
        LOG.info("calling auth-server delete client endpoint {}", deleteEndpoint);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().delete().uri(deleteEndpoint.toString())
                .retrieve();

        return responseSpec.bodyToMono(String.class).map(string-> {
            LOG.info("got back response from auth-server delete client call: {}", string);
            return string;
        }).onErrorResume(throwable -> {
            StringBuilder errorMessage = new StringBuilder("auth-server delete client failed: ")
                    .append(throwable.getMessage());
            LOG.error(errorMessage.toString());
            return Mono.just(errorMessage.toString());
        });
    }

    /**
     * this will call authorization-server clients endpoints to get
     * clientIds for a given userId <a href="http://authorization-server/clients/">...</a>{userId}
     *
     * @param userId user id of user
     * @return return a list of clientId strings
     */
    @Override
    public Mono<List<String>> getUserClientIds(UUID userId) {
        LOG.info("get user '{}' clients", userId);

        StringBuilder clientsEndpoint = new StringBuilder(this.clientsEndpoint).append("/").append(userId.toString());
        LOG.info("calling auth-server get clientIds for userId endpoint {}", clientsEndpoint);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().delete().uri(clientsEndpoint.toString())
                .retrieve();

        return responseSpec.bodyToMono(new ParameterizedTypeReference<List<String>>() {}).map(list-> {
            LOG.info("got back response from auth-server get clientIds for userId call: {}", list);
            return list;
        }).onErrorResume(throwable -> {
            String errorMessage = "auth-server get clientIds for userId failed: " +
                    throwable.getMessage();
            LOG.error(errorMessage);
            return Mono.error(throwable);
        });
    }


}
