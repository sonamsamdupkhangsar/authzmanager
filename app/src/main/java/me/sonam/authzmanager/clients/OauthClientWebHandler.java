package me.sonam.authzmanager.clients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
public class OauthClientWebHandler implements OauthClientHandler {
    private static final Logger LOG = LoggerFactory.getLogger(OauthClientWebHandler.class);

    private final OauthClientRoute oauthClientRoute;
    public OauthClientWebHandler(OauthClientRoute oauthClientRoute) {
        this.oauthClientRoute = oauthClientRoute;
    }

    @Override
    public Mono<ServerResponse> createClient(ServerRequest serverRequest) {
        LOG.info("create client");
        return serverRequest.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .flatMap(oauthClientRoute::createClient)
                .flatMap(s -> {
                    LOG.info("create client response: {}", s);
                    return ServerResponse.created(URI.create("/authzmanager/clients"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("message", s));
                })
                .onErrorResume(throwable -> {
                    LOG.error("create client failed, message: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", "failed to create client: "+ throwable.getMessage()));
                });
    }

    @Override
    public Mono<ServerResponse> updateClient(ServerRequest serverRequest) {
        LOG.info("update client");
        return serverRequest.bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {})
                //.flatMap(map -> oauthClientRoute.updateClient(map))
                .flatMap(s -> {
                    LOG.info("update client response: {}", s);
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("message", s));
                })
                .onErrorResume(throwable -> {
                    LOG.error("client update failed, message: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", "failed to update client: "+ throwable.getMessage()));
                });
    }

    @Override
    public Mono<ServerResponse> deleteClient(ServerRequest serverRequest) {
        LOG.info("delete client");
        return oauthClientRoute.deleteClient(UUID.fromString(serverRequest.pathVariable("id")),
                        UUID.fromString(serverRequest.pathVariable("ownerId")))
                .flatMap(s -> {
                    LOG.info("client deleted, response: {}", s);
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("message", s));
                })
                .onErrorResume(throwable -> {
                    LOG.error("failed to delete client, message: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", "failed to delete client: "+ throwable.getMessage()));
                });

    }

    @Override
    public Mono<ServerResponse> getUserOauth2Clients(ServerRequest serverRequest) {
        LOG.info("get user Oauth clients");
        return oauthClientRoute.getUserClientIds(UUID.fromString(serverRequest.pathVariable("userId")))
                .flatMap(s -> {
                    LOG.debug("retrieved user Oauth2 clients, response: {}", s);
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("clients", s));
                })
                .onErrorResume(throwable -> {
                    LOG.error("failed to get user Oauth2 clients, message: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", "failed to get Oauth2 clients: "+ throwable.getMessage()));
                });
    }
}
