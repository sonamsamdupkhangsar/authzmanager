package me.sonam.authzmanager.clients;

import me.sonam.authzmanager.controller.admin.oauth2.RegisteredClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface OauthClientRoute {

    Mono<RegisteredClient> createClient(Map<String, Object> map);
    Mono<RegisteredClient> updateClient(Map<String, Object> map);
    Mono<Void> deleteClient(String clientId, UUID ownerId);
    Mono<List<String>> getUserClientIds(UUID userId);
    //Mono<Map<String, Object>> getOauthClientByClientId(String clientId);
    Mono<RegisteredClient> getOauthClientByClientId(String clientId);
}
