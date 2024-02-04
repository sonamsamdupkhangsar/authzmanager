package me.sonam.authzmanager.clients;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface OauthClientRoute {

    Mono<String> createClient(Map<String, String> map);
    Mono<String> updateClient(Map<String, String> map);
    Mono<String> deleteClient(String clientId);
    Mono<List<String>> getUserClientIds(UUID userId);
}
