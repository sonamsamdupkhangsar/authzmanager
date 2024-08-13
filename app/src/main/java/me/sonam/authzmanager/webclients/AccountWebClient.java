package me.sonam.authzmanager.webclients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.UUID;

public class AccountWebClient {
    private static final Logger LOG = LoggerFactory.getLogger(AccountWebClient.class);

    private final WebClient.Builder webClientBuilder;

    private final String deleteMyAccount;

    public AccountWebClient(WebClient.Builder webClientBuilder,
                            String deleteMyAccount) {
        this.webClientBuilder = webClientBuilder;
        this.deleteMyAccount = deleteMyAccount;
    }

    public Mono<String> deleteMyAccount(String accessToken) {
        LOG.info("delete my account using accessToken");
        WebClient.ResponseSpec responseSpec = webClientBuilder.build().delete().uri(deleteMyAccount)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .retrieve();
        return responseSpec.bodyToMono(String.class);
    }
}
