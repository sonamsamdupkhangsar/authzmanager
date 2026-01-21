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
import java.util.Optional;
import java.util.UUID;

public class UserWebClient {
    private static final Logger LOG = LoggerFactory.getLogger(UserWebClient.class);
    private final WebClient.Builder webClientBuilder;
    private final String userRestServiceEndpoint;
    private final String profilePhotoEndpoint;

    public UserWebClient(WebClient.Builder webClientBuilder, String userRestServiceEndpoint, String profilePhotoEndpoint) {
        this.webClientBuilder = webClientBuilder;
        this.userRestServiceEndpoint = userRestServiceEndpoint;
        this.profilePhotoEndpoint = profilePhotoEndpoint;
    }

    public Mono<Map> signupUser(String accessToken, UserSignup userSignup) {
        LOG.info("create user with endpoint: {}", userRestServiceEndpoint);

        WebClient.RequestBodySpec requestBodySpec = webClientBuilder.build().post().uri(userRestServiceEndpoint);

        if (accessToken != null) {
            LOG.info("add access-token in header");
            requestBodySpec.headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken));
        }

        WebClient.ResponseSpec responseSpec = requestBodySpec
                .bodyValue(userSignup)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve();

        return responseSpec.bodyToMono(Map.class);

    }

    public Mono<List<User>> getUserByBatchOfIds(String accessToken, List<UUID> ids) {
        StringBuilder stringBuilder = new StringBuilder(userRestServiceEndpoint).append("/ids/");
        for(int i = 0; i < ids.size(); i++) {
            stringBuilder.append(ids.get(i));
            if (i+1 < ids.size()) {
                stringBuilder.append(",");
            }
        }
        String batchIdEnpoint = stringBuilder.toString();

        LOG.info("get batch of users by ids with endpoint: {}", batchIdEnpoint);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(batchIdEnpoint)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve();
        return responseSpec.bodyToMono(new ParameterizedTypeReference<List<User>>() {})
                .onErrorResume(throwable -> {
            LOG.error("no users with id: {}", ids, throwable);
            return Mono.empty();
        });

    }

    public Mono<User> getUserById(String accessToken, UUID id) {
        LOG.info("calling user-rest-service to get user by id: {}", id);

        StringBuilder stringBuilder = new StringBuilder(userRestServiceEndpoint).append("/").append(id);

        LOG.info("user endpoint: {}", stringBuilder);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve();
        return responseSpec.bodyToMono(User.class);
    }

    /**
     * this is for finding user by authenticationId for application purpose
     * @param accessToken
     * @param authenticationId
     * @return
     */
    public Mono<User> findByAuthenticationId(String accessToken, String authenticationId) {
        LOG.info("find user by authenticationId: {}", authenticationId);

        StringBuilder stringBuilder = new StringBuilder(userRestServiceEndpoint).append("/authentication-id/")
                .append(authenticationId);

        LOG.info("find user with authentication-id with endpoint: {}", stringBuilder);
        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve();

        return responseSpec.bodyToMono(User.class).onErrorResume(throwable -> {
            LOG.error("user not found with authenticationId: {}", authenticationId, throwable.getMessage());
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

    /**
     * this search is dependent upon if the user has turned off profile searching by others
     * @param accessToken
     * @param authenticationId
     * @return
     */
    public Mono<User> findByAuthenticationProfileSearch(String accessToken, String authenticationId) {
        LOG.info("find user profile search by authenticationId: {}", authenticationId);

        StringBuilder stringBuilder = new StringBuilder(userRestServiceEndpoint).append("/profile/authentication-id/")
                .append(authenticationId).append("?ignoreSearchable=true");  //this queryParam is to search regardless of user setting

        LOG.info("endpoint: {}", stringBuilder);
        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .accept(MediaType.APPLICATION_JSON)
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

    public Mono<String> updateProfile(String accessToken, User user) {
        LOG.info("update user profile using accessToken: {}", accessToken);

        StringBuilder stringBuilder = new StringBuilder(userRestServiceEndpoint);

        LOG.info("update profile with endpoint: {}", stringBuilder);
        WebClient.ResponseSpec responseSpec = webClientBuilder.build().put().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .bodyValue(user).accept(MediaType.APPLICATION_JSON)
                .retrieve();

        return responseSpec.bodyToMono(String.class);
    }

    public Mono<String> updateProfilePhoto(String accessToken, User user) {
        LOG.info("update user profile photo using accessToken: {}", accessToken);

        StringBuilder stringBuilder = new StringBuilder(profilePhotoEndpoint);

        LOG.info("endpoint: {}", stringBuilder);
        WebClient.ResponseSpec responseSpec = webClientBuilder.build().put().uri(stringBuilder.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .bodyValue(user).accept(MediaType.APPLICATION_JSON)
                .retrieve();

        return responseSpec.bodyToMono(String.class);
    }


    public Mono<String> deleteUser(String accessToken, UUID organizationId) {
        LOG.info("delete user information {}", userRestServiceEndpoint);
        final String endpoint = userRestServiceEndpoint + "/" +organizationId;
        WebClient.ResponseSpec responseSpec = webClientBuilder.build().delete().uri(endpoint)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve();
        return responseSpec.bodyToMono(String.class).thenReturn("User deletion success");
    }

}
