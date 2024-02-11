package me.sonam.authzmanager.user;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
public class UserRouteAuthServer implements UserRoute {
    private static final Logger LOG = LoggerFactory.getLogger(UserRouteAuthServer.class);
    private final String userSignupEndpoint;

    private final String authenticateEndpoint;
    private final WebClient.Builder webClientBuilder;

    public UserRouteAuthServer(WebClient.Builder webClientBuilder, String userSignupEndpoint, String authenticateEndpoint) {
        this.webClientBuilder = webClientBuilder;
        this.userSignupEndpoint = userSignupEndpoint;
        this.authenticateEndpoint = authenticateEndpoint;
    }

    @Override
    public Mono<String> signupUser(Map<String, String> map) {
        LOG.info("signup user by calling external user-service");

        return signupUserRestService(map);
    }

    @Override
    public Mono<String> authenticate(Map<String, String> map) {
        LOG.info("authenitcation by calling external authentication-rest-service");

       return authenticationUserRestService(map);
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

    private Mono<String> authenticationUserRestService(Map<String, String> signupMap) {
        LOG.info("calling user-rest-service authenticate endpoint {}", authenticateEndpoint);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().post().uri(authenticateEndpoint)
                .bodyValue(signupMap).retrieve();


        return responseSpec.bodyToMono(Map.class).map(responseMap-> {
            LOG.info("got back response from user-rest-service call: {}", responseMap.get("message"));
            if (responseMap.get("message") !=  null) {
                String roleList = responseMap.get("roleName").toString();
                roleList = roleList.replace("[", "");
                roleList = roleList.replace("]", "");

                return roleList;
            }
            else {
                return responseMap.get("error").toString();
            }
        }).onErrorResume(throwable -> {
            LOG.error("authentication failed: {}", throwable.getMessage());
            return Mono.just("authentication failed: "+ throwable.getMessage());
        });
    }
}
