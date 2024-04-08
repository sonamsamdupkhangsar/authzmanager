package me.sonam.authzmanager.clients;

import me.sonam.authzmanager.controller.signup.UserSignup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class UserWebClient {
    private static final Logger LOG = LoggerFactory.getLogger(UserWebClient.class);
    private WebClient.Builder webClientBuilder;
    private String userRestServiceEndpoint;
    public UserWebClient(WebClient.Builder webClientBuilder, String userRestServiceEndpoint) {
        this.webClientBuilder = webClientBuilder;
        this.userRestServiceEndpoint = userRestServiceEndpoint;
    }

    public Mono<String> signupUser(UserSignup userSignup) {
        LOG.info("calling user-rest-serivce endpoint: {}", userRestServiceEndpoint);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().post().uri(userRestServiceEndpoint)
                .bodyValue(userSignup)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve();
        return responseSpec.bodyToMono(String.class).thenReturn("User signup success");

    }
}
