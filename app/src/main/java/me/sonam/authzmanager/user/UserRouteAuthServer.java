package me.sonam.authzmanager.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
public class UserRouteAuthServer implements UserRoute {
    private static final Logger LOG = LoggerFactory.getLogger(UserRouteAuthServer.class);
    private final String userSignupEndpoint;

    private final WebClient.Builder webClientBuilder;

    public UserRouteAuthServer(WebClient.Builder webClientBuilder, String userSignupEndpoint) {
        this.webClientBuilder = webClientBuilder;
        this.userSignupEndpoint = userSignupEndpoint;
    }

    @Override
    public Mono<String> signupUser(Map<String, String> map) {
        LOG.info("signup user by calling external user-service");

        return signupUserRestService(map);
    }

    private Mono<String> signupUserRestService(Map<String, String> signupMap) {
        LOG.info("calling user-rest-service signup endpoint {}", userSignupEndpoint);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().post().uri(userSignupEndpoint)
                .bodyValue(signupMap).retrieve();

        return responseSpec.bodyToMono(Map.class).map(responseMap-> {
            LOG.info("got back response from user-rest-service call: {}", responseMap.get("message"));
            return responseMap.get("message").toString();
        }).onErrorResume(throwable -> {
            LOG.error("user-rest-service failed: {}", throwable.getMessage());

            return Mono.just("signup failed: "+ throwable.getMessage());
        });
    }

}
