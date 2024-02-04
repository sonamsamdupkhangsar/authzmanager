package me.sonam.authzmanager.user;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public interface UserHandler {
    Mono<ServerResponse> authenticate(ServerRequest serverRequest);
    Mono<ServerResponse> createUser(ServerRequest serverRequest);
}
