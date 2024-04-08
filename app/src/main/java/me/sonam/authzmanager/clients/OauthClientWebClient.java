package me.sonam.authzmanager.clients;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.sonam.authzmanager.controller.admin.oauth2.AuthorizationGrantType;
import me.sonam.authzmanager.controller.admin.oauth2.ClientAuthenticationMethod;
import me.sonam.authzmanager.controller.admin.oauth2.ClientSettings;
import me.sonam.authzmanager.controller.admin.oauth2.RegisteredClient;
import me.sonam.authzmanager.controller.admin.oauth2.util.RegisteredClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

public class OauthClientWebClient implements OauthClientRoute {
    private static final Logger LOG = LoggerFactory.getLogger(OauthClientWebClient.class);

    private final String clientsEndpoint;

    private final WebClient.Builder webClientBuilder;
    private RegisteredClientUtil registeredClientUtil = new RegisteredClientUtil();

    public OauthClientWebClient(WebClient.Builder webclientBuilder, String clientsEndpoint) {
        this.webClientBuilder = webclientBuilder;
        this.clientsEndpoint = clientsEndpoint;
    }
    @Override
    public Mono<RegisteredClient> createClient(Map<String, Object> map) {
        LOG.info("create client");

        LOG.info("calling auth-server create client endpoint {}", clientsEndpoint);

        LOG.info("payload: {}", map);

        //LOG.info("requestBody: {}", requestBody);
        WebClient.ResponseSpec responseSpec = webClientBuilder.build().post().uri(clientsEndpoint)
                .bodyValue(map).retrieve();
        return responseSpec.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>(){}).map(responseMap-> {
            LOG.info("got back response from auth-server create client call: {}", responseMap);
            return registeredClientUtil.build(responseMap);
            //return responseMap;
        }).onErrorResume(throwable -> {
            String stringBuilder = "auth-server create client failed: " +
                    throwable.getMessage();
            LOG.error(stringBuilder, throwable);
            //return Mono.just(Map.of("error", stringBuilder));
            return Mono.error(throwable);
        });
    }

    @Override
    public Mono<RegisteredClient> updateClient(Map<String, Object> map) {
        LOG.info("update client");

        LOG.info("calling auth-server update client endpoint {}", clientsEndpoint);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().put().uri(clientsEndpoint)
                .bodyValue(map).retrieve();
        return responseSpec.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>(){}).map(responseMap-> {
            LOG.info("got back response from auth-server update client call: {}", responseMap);

            return registeredClientUtil.build(responseMap);
        }).onErrorResume(throwable -> {
            String stringBuilder = "auth-server update client failed: " +
                    throwable.getMessage();
            LOG.error(stringBuilder, throwable);
            return Mono.error(throwable);
        });
    }

    @Override
    public Mono<Void> deleteClient(String clientId, UUID ownerId) {
        LOG.info("delete client");

        StringBuilder deleteEndpoint = new StringBuilder(clientsEndpoint).append("/")
                .append(clientId).append("/ownerId/").append(ownerId);
        LOG.info("calling auth-server delete client endpoint {}", deleteEndpoint);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().delete().uri(deleteEndpoint.toString())
                .retrieve();

        return responseSpec.bodyToMono(String.class).then();
    }

    /**
     * this will call authorization-server clients endpoints to get
     * clientIds for a given userId <a href="http://authorization-server/clients/">...</a>{userId}
     *
     * @param userId user id of user
     * @return return a list of clientId strings
     */
    @Override
    public Mono<List<String>> getUserClientIds(UUID userId) {
        LOG.info("get user '{}' clients", userId);

        StringBuilder clientsEndpoint = new StringBuilder(this.clientsEndpoint).append("/user/").append(userId.toString());
        LOG.info("calling auth-server get clientIds for userId endpoint {}", clientsEndpoint);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(clientsEndpoint.toString())
                .retrieve();

        return responseSpec.bodyToMono(new ParameterizedTypeReference<List<String>>() {}).map(list-> {
            LOG.info("got back response from auth-server get clientIds for userId call: {}", list);
            return list;
        }).onErrorResume(throwable -> {
            String errorMessage = "auth-server get clientIds for userId failed: " +
                    throwable.getMessage();
            LOG.error(errorMessage);
            return Mono.error(throwable);
        });
    }

    @Override
    public Mono<RegisteredClient> getOauthClientByClientId(String clientId) {
        LOG.info("get oauthClient by clientId {}", clientId);

        StringBuilder clientsEndpoint = new StringBuilder(this.clientsEndpoint).append("/").append(clientId);
        LOG.info("calling auth-server get clientId by clientId with endpoint {}", clientsEndpoint);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().get().uri(clientsEndpoint.toString())
                .retrieve();
        return responseSpec.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {}).map(map-> {
            LOG.info("got back response from auth-server get clientId by clientId  call: {}", map);


            return registeredClientUtil.build(map);
        }).onErrorResume(throwable -> {
            String errorMessage = "auth-server get clientId by clientId failed: " +
                    throwable.getMessage();
            LOG.error(errorMessage);
            return Mono.error(throwable);
        });
    }

}
