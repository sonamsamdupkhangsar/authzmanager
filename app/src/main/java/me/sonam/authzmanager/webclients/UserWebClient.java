package me.sonam.authzmanager.webclients;

import me.sonam.authzmanager.AuthzManagerException;
import me.sonam.authzmanager.clients.user.User;
import me.sonam.authzmanager.controller.signup.UserSignup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UserWebClient {
    private static final Logger LOG = LoggerFactory.getLogger(UserWebClient.class);
    private WebClient.Builder webClientBuilder;
    private String userRestServiceEndpoint;
    public UserWebClient(WebClient.Builder webClientBuilder, String userRestServiceEndpoint) {
        this.webClientBuilder = webClientBuilder;
        this.userRestServiceEndpoint = userRestServiceEndpoint;
    }

    public Mono<String> signupUser(UserSignup userSignup) {
        LOG.info("calling user-rest-service endpoint: {}", userRestServiceEndpoint);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().post().uri(userRestServiceEndpoint)
                .bodyValue(userSignup)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve();
        return responseSpec.bodyToMono(String.class).thenReturn("User signup success");

    }

    public Mono<List<User>> getUserByBatchOfIds(List<UUID> ids) {
        LOG.info("calling user-rest-service to get batch of users by ids: {}", ids);

        StringBuilder stringBuilder = new StringBuilder(userRestServiceEndpoint).append("/ids/");
        for(int i = 0; i < ids.size(); i++) {
            stringBuilder.append(ids.get(i));
            if (i+1 < ids.size()) {
                stringBuilder.append(",");
            }
        }
        String batchIdEnpoint = stringBuilder.toString();

        LOG.info("user endpoint: {}", batchIdEnpoint);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(batchIdEnpoint)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve();
        return responseSpec.bodyToMono(new ParameterizedTypeReference<List<User>>() {})
                .onErrorResume(throwable -> {
            LOG.error("no users with id: {}", ids, throwable);
            return Mono.empty();
        });

    }

    public Mono<User> getUserById(UUID id) {
        LOG.info("calling user-rest-service to get user by id: {}", id);

        StringBuilder stringBuilder = new StringBuilder(userRestServiceEndpoint).append("/").append(id);

        LOG.info("user endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(stringBuilder.toString())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve();
        return responseSpec.bodyToMono(User.class);
    }


    public Mono<User> findByAuthentication(String authenticationId) {
        LOG.info("find user by authenticationId: {}", authenticationId);

        StringBuilder stringBuilder = new StringBuilder(userRestServiceEndpoint).append("/authentication-id/")
                .append(authenticationId);

        LOG.info("endpoint: {}", stringBuilder);
        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(stringBuilder.toString()).accept(MediaType.APPLICATION_JSON)
                .retrieve();

        return responseSpec.bodyToMono(User.class).onErrorResume(throwable -> {
            LOG.error("no user found authenticatioId: {}", authenticationId, throwable.getMessage());
            String errorMessage = throwable.getMessage();
            if (throwable instanceof WebClientResponseException) {
                WebClientResponseException webClientResponseException = (WebClientResponseException) throwable;
                LOG.error("error body contains: {}", webClientResponseException.getResponseBodyAsString());
                if (webClientResponseException.getResponseBodyAsString().contains("\"error\":"))
                {
                    Map<String, String> errorResponseMap = webClientResponseException.getResponseBodyAs(Map.class);
                    if (errorResponseMap != null) {
                        errorMessage = errorResponseMap.get("error");
                    }
                    else {
                        errorMessage = webClientResponseException.getResponseBodyAsString();
                    }
                }
            }

            return Mono.error(new AuthzManagerException(errorMessage));//Mono.just(new User());
        });
    }

    public Mono<String> updateProfile(User user) {
        LOG.info("update user profile");

        StringBuilder stringBuilder = new StringBuilder(userRestServiceEndpoint);

        LOG.info("endpoint: {}", stringBuilder);
        WebClient.ResponseSpec responseSpec = webClientBuilder.build().put().uri(stringBuilder.toString())
                .bodyValue(user).accept(MediaType.APPLICATION_JSON)
                .retrieve();

        return responseSpec.bodyToMono(String.class);
    }
}
