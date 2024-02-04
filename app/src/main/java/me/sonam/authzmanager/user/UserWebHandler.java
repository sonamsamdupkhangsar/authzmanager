package me.sonam.authzmanager.user;

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

@Service
public class UserWebHandler implements UserHandler {
    private static final Logger LOG = LoggerFactory.getLogger(UserWebHandler.class);

    private final UserRoute userRoute;

    public UserWebHandler(UserRoute userRoute) {
        this.userRoute = userRoute;
    }
    @Override
    public Mono<ServerResponse> createUser(ServerRequest serverRequest) {
        LOG.info("create user");

        return serverRequest.bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {})
                .flatMap(map -> userRoute.signupUser(map))
                .flatMap(s -> {
                    LOG.info("s contains: {}", s);
                    return ServerResponse.created(URI.create("/users/"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("message", s));
                })
                .onErrorResume(throwable -> {
                    LOG.error("authenticate failed, message: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", "user sign up failed: "+ throwable.getMessage()));
                });
    }
    @Override
    public Mono<ServerResponse> authenticate(ServerRequest serverRequest) {
        LOG.info("authenticate user");

        return serverRequest.bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {})
                .flatMap(stringStringMap -> userRoute.authenticate(stringStringMap))
                .flatMap(s -> {
                    LOG.info("authenticate response: {}", s);
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("message", "Authentication successful",
                                    "roleNames", s));
                })
                .onErrorResume(throwable -> {
                    LOG.error("authenticate failed, message: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

}
