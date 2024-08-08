package me.sonam.authzmanager.webclients;

import me.sonam.authzmanager.controller.signup.UserSignup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

public class TokenWebClient {
    private static final Logger LOG = LoggerFactory.getLogger(TokenWebClient.class);
    private final WebClient.Builder webClientBuilder;
    private final String tokenEndpoint;

    public TokenWebClient(WebClient.Builder webClientBuilder, String tokenEdnpoint) {
        this.webClientBuilder = webClientBuilder;
        this.tokenEndpoint = tokenEdnpoint;
    }

    public Mono<Map<String, String>> refreshToken(String refreshToken) {
        LOG.info("calling token endpoint: {}", tokenEndpoint);

        MultiValueMap<String, Object> multiValueMap = new LinkedMultiValueMap<>();
        final String REFRESH_TOKEN = "refresh_token";

        multiValueMap.add("grant_type", REFRESH_TOKEN);
        multiValueMap.add(REFRESH_TOKEN, refreshToken);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().post().uri(tokenEndpoint)
                .bodyValue(multiValueMap)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve();
        return responseSpec.bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {})
                .onErrorResume(throwable -> {
                    LOG.error("failed to get refresh token", throwable);
                    return Mono.just(Map.of("error", "failed to get access token for refresh token: "
                            + throwable.getMessage()));
                }
                );
    }
}
