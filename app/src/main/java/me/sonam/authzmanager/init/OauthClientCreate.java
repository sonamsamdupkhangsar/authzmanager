package me.sonam.authzmanager.init;

import jakarta.annotation.PostConstruct;
import me.sonam.authzmanager.clients.OauthClientRoute;
import me.sonam.authzmanager.controller.admin.oauth2.AuthorizationGrantType;
import me.sonam.authzmanager.controller.admin.oauth2.OauthClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Configuration
public class OauthClientCreate {

    private static final Logger LOG = LoggerFactory.getLogger(OauthClientCreate.class);
    private final OauthClientRoute oauthClientRoute;

    @Value("${oauthClientId}")
    private String oauthClientId;

    public OauthClientCreate(OauthClientRoute oauthClientRoute) {
        this.oauthClientRoute = oauthClientRoute;
    }
    /**
     * this creates a OAuthClient for this app.
     */
    //@PostConstruct
   /* public void createOAuthClient() {
        LOG.info("create oauthClient for this application");
        OauthClient oauthClient = new OauthClient();
        oauthClient.setClientId(oauthClientId);
        oauthClient.setClientSecret("{noop}secret");
        oauthClient.setClientSecretExpiresAt("");
        oauthClient.setClientName("");
        oauthClient.setPostLogoutRedirectUris("");
        oauthClient.setRedirectUris("");
        oauthClient.setClientIdIssuedAt("");
        oauthClient.setClientSettings("settings.client.require-authorization-consent=true,settings.client.require-proof-key=false");
        List<String> list = new ArrayList<>();
        list.add("REFRESH_TOKEN");

        oauthClient.setAuthorizationGrantTypes(list);

        Map<String, Object> map = oauthClient.getMap();

        map.put("userId", UUID.randomUUID());

        Mono<Map<String, Object>> mapCreated = oauthClientRoute.createClient(map);
        mapCreated.subscribe(stringStringMap -> LOG.info("response from oauthserver is: {}", stringStringMap));
    }*/
}
