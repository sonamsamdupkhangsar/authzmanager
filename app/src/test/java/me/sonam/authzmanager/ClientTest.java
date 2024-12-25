package me.sonam.authzmanager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.sonam.authzmanager.oauth2.*;
import me.sonam.authzmanager.oauth2.util.RegisteredClientUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ClientTest {
    private static final Logger LOG = LoggerFactory.getLogger(ClientTest.class);

    @Test
    public void clientMap() throws JsonProcessingException {
        final String response = "{redirectUris=http://www.hello.com, scopes=email, clientId=hello, clientSecret=, clientSettings={\"@class\":\"java.util.Collections$UnmodifiableMap\",\"settings.client.require-proof-key\":\"false\"}, clientName=114775bc-9c0b-48e1-872e-a46f9cd2d0e9, id=114775bc-9c0b-48e1-872e-a46f9cd2d0e9, clientAuthenticationMethods=client_secret_basic, authorizationGrantTypes=client_credentials, tokenSettings={\"@class\":\"java.util.Collections$UnmodifiableMap\",\"settings.token.reuse-refresh-tokens\":true,\"settings.token.id-token-signature-algorithm\":[\"org.springframework.security.oauth2.jose.jws.SignatureAlgorithm\",\"RS256\"],\"settings.token.access-token-time-to-live\":[\"java.time.Duration\",300.000000000],\"settings.token.access-token-format\":{\"@class\":\"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat\",\"value\":\"self-contained\"},\"settings.token.refresh-token-time-to-live\":[\"java.time.Duration\",3600.000000000],\"settings.token.authorization-code-time-to-live\":[\"java.time.Duration\",300.000000000],\"settings.token.device-code-time-to-live\":[\"java.time.Duration\",300.000000000]}}";

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, Object> map = objectMapper.readValue(response, Map.class);
            LOG.info("map: {}", map);
        }
        catch (Exception e) {
            LOG.error("Failed to convert to map", e);
        }

    }

    @Test
    public void oauthClientToRegisteredClient() {
        RegisteredClient registeredClient = save("hello", "secret");
        LOG.info("created registered client: {}",registeredClient);


        RegisteredClientUtil registeredClientUtil = new RegisteredClientUtil();
        LOG.info("get map from registered client");
        Map<String, Object> map = registeredClientUtil.getMapObject(registeredClient);
        LOG.info("get registeredClient back from map: {}", map);
        RegisteredClient registeredClientFromMap = registeredClientUtil.build(map);
        LOG.info("get oauthClient from registeredClientMap: {}", registeredClientFromMap);

        OauthClient oauthClient = OauthClient.getFromRegisteredClient(registeredClientFromMap);
        assertThat(oauthClient).isNotNull();

    }

    @Test
    public void listIntersect() {
        List<String> standardList = List.of("apple", "ball", "cat", "dog", "elephant");
        List<String> chosen = new ArrayList<>(List.of("ball", "goat", "mouse"));
        chosen.removeAll(standardList);
        LOG.info("chosen contains: {}", chosen);

        assertThat(chosen.contains("goat")).isTrue();
        assertThat(chosen.contains("mouse")).isTrue();

    }

    @Test
    public void containsAuthorizationGrantTypes() {
        OauthClient oauthClient = new OauthClient();
        oauthClient.getAuthorizationGrantTypes().add("AUTHORIZATION_CODE");

        LOG.info("authcode: {}", AuthorizationGrantType.AUTHORIZATION_CODE.getValue().toUpperCase());
        if (oauthClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.AUTHORIZATION_CODE.getValue().toUpperCase())) {
            LOG.info("contains auth code");
        }
        else {
            LOG.info("not contains");
        }
    }
    //@Test
    public void createRegisteredClient() {
        //RegisteredClient registeredClient = save("hello", "password");
        RegisteredClientUtil registeredClientUtil = new RegisteredClientUtil();
        OauthClient oauthClient = new OauthClient();
        oauthClient.setId(UUID.randomUUID().toString());
        oauthClient.setClientId("name");
        oauthClient.setRedirectUris("");

       /* oauthClient.setAuthorizationGrantTypes(List.of(AuthorizationGrantType.AUTHORIZATION_CODE.getValue().toUpperCase()));
        oauthClient.setAuthenticationMethods(List.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue().toUpperCase()));
        RegisteredClient registeredClient = oauthClient.getInitialRegisteredClient();
        LOG.info("initialRegistedClient: {}", registeredClient);*/

    }

    @Test
    public void registeredClient() {
       /* final String registeredClientMap = "{id='a22199ce-595e-4842-a31d-266f5db9ed51', clientId='testclient', clientName='a22199ce-595e-4842-a31d-266f5db9ed51', clientAuthenticationMethods=[org.springframework.security.oauth2.core.ClientAuthenticationMethod@35e4e7ac, org.springframework.security.oauth2.core.ClientAuthenticationMethod@4fcef9d3], authorizationGrantTypes=[org.springframework.security.oauth2.core.AuthorizationGrantType@aaa4df95, org.springframework.security.oauth2.core.AuthorizationGrantType@114a1c88, org.springframework.security.oauth2.core.AuthorizationGrantType@5da5e9f3], redirectUris=[http://127.0.0.1:8080/authorized, http://127.0.0.1:8080/login/oauth2/code/messaging-client-oidc], postLogoutRedirectUris=[], scopes=[openid, profile, message.read, message.write], clientSettings=AbstractSettings {settings={settings.client.require-proof-key=false, settings.client.require-authorization-consent=true}}, tokenSettings=AbstractSettings {settings={settings.token.reuse-refresh-tokens=true, settings.token.id-token-signature-algorithm=RS256, settings.token.access-token-time-to-live=PT5M, settings.token.access-token-format=org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat@face4f32, settings.token.refresh-token-time-to-live=PT1H, settings.token.authorization-code-time-to-live=PT5M, settings.token.device-code-time-to-live=PT5M}}}";
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            RegisteredClient registeredClient = objectMapper.readValue(registeredClientMap, RegisteredClient.class);
            LOG.info("map: {}", registeredClient);
        }
        catch (Exception e) {
            LOG.error("Failed to convert to map", e);
        }*/

        RegisteredClient registeredClient = save("client-id", "mysecret");

        assertThat(registeredClient.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)).isTrue();

    }

    private RegisteredClient save(String clientId, String clientSecret) {

        RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret("{noop}"+clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_JWT)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .redirectUri("http://127.0.0.1:8080/login/oauth2/code/messaging-client-oidc")
                .redirectUri("http://127.0.0.1:8080/authorized")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("message.read")
                .scope("message.write")
               // .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).requireProofKey(false).build())
                .build();
        return  registeredClient;
    }

    @Test
    public void createTest() {
        Set<ClientAuthenticationMethod> clientAuthenticationMethodSet = new HashSet<>();

        clientAuthenticationMethodSet.add(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
        clientAuthenticationMethodSet.add(ClientAuthenticationMethod.CLIENT_SECRET_POST);
        clientAuthenticationMethodSet.add(ClientAuthenticationMethod.CLIENT_SECRET_JWT);
        clientAuthenticationMethodSet.add(ClientAuthenticationMethod.NONE);


        RegisteredClient.Builder registeredClientBuilder = RegisteredClient.withId("hello").clientId("hello-client")
                .clientSecret("secret");
        for (ClientAuthenticationMethod cam : clientAuthenticationMethodSet) {
            registeredClientBuilder.clientAuthenticationMethod(cam);
        }

        Set<AuthorizationGrantType> authorizationGrantTypeSet = new HashSet<>();

        authorizationGrantTypeSet.add(AuthorizationGrantType.AUTHORIZATION_CODE);
        authorizationGrantTypeSet.add(AuthorizationGrantType.CLIENT_CREDENTIALS);
        authorizationGrantTypeSet.add(AuthorizationGrantType.REFRESH_TOKEN);
        authorizationGrantTypeSet.add(AuthorizationGrantType.DEVICE_CODE);
        authorizationGrantTypeSet.add(AuthorizationGrantType.TOKEN_EXCHANGE);
        authorizationGrantTypeSet.add(AuthorizationGrantType.JWT_BEARER);

        for(AuthorizationGrantType authorizationGrantType: authorizationGrantTypeSet) {
            registeredClientBuilder.authorizationGrantType(authorizationGrantType);
            LOG.info("add grant types");
        }

        registeredClientBuilder.redirectUri("http://hello.com");

        LOG.info("registeredClient: {}", registeredClientBuilder.build());

    }

    public Map<String, String> convertStringToMap(String data) {
        Map<String, String> map = new HashMap<>();
        StringTokenizer tokenizer = new StringTokenizer(data, " ");

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            String[] keyValue = token.split("=");
            map.put(keyValue[0], keyValue[1]);
        }

        return map;
    }
}
