package me.sonam.authzmanager.user;

import reactor.core.publisher.Mono;

import java.util.Map;

public interface UserRoute {
    Mono<String> signupUser(Map<String, String> map);
    Mono<String> authenticate(Map<String, String> map);
}
