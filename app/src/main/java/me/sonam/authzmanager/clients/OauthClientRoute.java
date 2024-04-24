package me.sonam.authzmanager.clients;

import me.sonam.authzmanager.controller.admin.oauth2.RegisteredClient;
import me.sonam.authzmanager.controller.util.MyPair;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface OauthClientRoute {

    Mono<RegisteredClient> createClient(Map<String, Object> map);
    Mono<RegisteredClient> updateClient(Map<String, Object> map, HttpMethod httpMethod);
    Mono<Void> deleteClient(UUID id, UUID ownerId);
    Mono<List<MyPair<String, String>>> getUserClientIds(UUID userId);
    //Mono<Map<String, Object>> getOauthClientByClientId(String clientId);
    Mono<RegisteredClient> getOauthClientByClientId(String clientId);
    Mono<RegisteredClient> getOauthClientById(UUID id);
}
