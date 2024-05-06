package me.sonam.authzmanager.controller.admin.oauth2;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;

import java.time.Duration;
import java.util.*;

public class OauthClient {
    private static final Logger LOG = LoggerFactory.getLogger(OauthClient.class);
    private String id;
    @NotEmpty(message="Your clientId value must be greater than 2 characters")
    @Size(min = 3, max = 100)
    private String clientId;
    private UUID clientIdUuid;
    private String fullClientId;
    private String clientIdIssuedAt;
    //@NotEmpty(message="secret cannot be empty")
    private String clientSecret;
    private String clientSecretExpiresAt;
    private String clientName;
    //private String clientAuthenticationMethods;
    private List<String> clientAuthenticationMethods = new ArrayList<>();
    @NotEmpty(message="Need to select at least one of Authorization grant types")
    private List<String> authorizationGrantTypes = new ArrayList<>();
    //private String authorizationGrantTypes;

    private List<String> authenticationMethods = new ArrayList<>();
    private List<String> grantTypes = new ArrayList<>();
    private OidcScopes oidcScopes;
    //@NotEmpty(message="redirect uris cannot be empty")
    @Size(max = 500)
    private String redirectUris="";
    private String postLogoutRedirectUris;
    private List<String> scopes = new ArrayList<>();
    @Size(max = 200)
    private String customScopes;

    //private String scopes;

    //private Map<String, Boolean> clientSettings = new HashMap<>();
   // private String clientSettings;
    private ClientSettings clientSettings = new ClientSettings();

    private TokenSettings tokenSettings = new TokenSettings();
    static class ClientSettings {
        private boolean requireAuthorizationConsent;
        private boolean requireProofKey;
        private String jwkSetUrl;

        public ClientSettings() {

        }
        public boolean isRequireAuthorizationConsent() {
            return requireAuthorizationConsent;
        }

        public void setRequireAuthorizationConsent(boolean requireAuthorizationConsent) {
            this.requireAuthorizationConsent = requireAuthorizationConsent;
        }

        public boolean isRequireProofKey() {
            return requireProofKey;
        }

        public void setRequireProofKey(boolean requireProofKey) {
            this.requireProofKey = requireProofKey;
        }

        public String getJwkSetUrl() {
            return jwkSetUrl;
        }

        public void setJwkSetUrl(String jwkSetUrl) {
            this.jwkSetUrl = jwkSetUrl;
        }

        @Override
        public String toString() {
            return "ClientSettings{" +
                    "requireAuthorizationConsent=" + requireAuthorizationConsent +
                    ", requireProofKey=" + requireProofKey +
                    ", jwkSetUrl='" + jwkSetUrl + '\'' +
                    '}';
        }
    }



    public static class TokenSettings {
        //long represents the duration in seconds

        private long authorizationCodeTimeToLive;

        private long accessTokenTimeToLive;
        private OAuth2TokenFormat accessTokenFormat;

        private long deviceCodeTimeToLive;
        private boolean reuseRefreshTokens;

        private long refreshTokenTimeToLive;
        private SignatureAlgorithm idTokenSignatureAlgorithm;

        public TokenSettings() {
        }

        public long getAuthorizationCodeTimeToLive() {
            return authorizationCodeTimeToLive;
        }

        public void setAuthorizationCodeTimeToLive(long authorizationCodeTimeToLive) {
            this.authorizationCodeTimeToLive = authorizationCodeTimeToLive;
        }

        public long getAccessTokenTimeToLive() {
            return accessTokenTimeToLive;
        }

        public void setAccessTokenTimeToLive(long accessTokenTimeToLive) {
            this.accessTokenTimeToLive = accessTokenTimeToLive;
        }

        public OAuth2TokenFormat getAccessTokenFormat() {
            return accessTokenFormat;
        }

        public void setAccessTokenFormat(OAuth2TokenFormat accessTokenFormat) {
            this.accessTokenFormat = accessTokenFormat;
        }

        public long getDeviceCodeTimeToLive() {
            return deviceCodeTimeToLive;
        }

        public void setDeviceCodeTimeToLive(long deviceCodeTimeToLive) {
            this.deviceCodeTimeToLive = deviceCodeTimeToLive;
        }

        public boolean isReuseRefreshTokens() {
            return reuseRefreshTokens;
        }

        public void setReuseRefreshTokens(boolean reuseRefreshTokens) {
            this.reuseRefreshTokens = reuseRefreshTokens;
        }

        public long getRefreshTokenTimeToLive() {
            return refreshTokenTimeToLive;
        }

        public void setRefreshTokenTimeToLive(long refreshTokenTimeToLive) {
            this.refreshTokenTimeToLive = refreshTokenTimeToLive;
        }

        public SignatureAlgorithm getIdTokenSignatureAlgorithm() {
            return idTokenSignatureAlgorithm;
        }

        public void setIdTokenSignatureAlgorithm(SignatureAlgorithm tokenSignatureAlgorithm) {
            this.idTokenSignatureAlgorithm = tokenSignatureAlgorithm;
        }

        @Override
        public String toString() {
            return "TokenSettings{" +
                    "authorizationCodeTimeToLive=" + authorizationCodeTimeToLive +
                    ", accessTokenTimeToLive=" + accessTokenTimeToLive +
                    ", accessTokenFormat=" + accessTokenFormat +
                    ", deviceCodeTimeToLive=" + deviceCodeTimeToLive +
                    ", reuseRefreshTokens=" + reuseRefreshTokens +
                    ", refreshTokenTimeToLive=" + refreshTokenTimeToLive +
                    ", idTokenSignatureAlgorithm=" + idTokenSignatureAlgorithm +
                    '}';
        }
    }
    private boolean mediateToken;
    private List<String> availableScopes = new ArrayList<>();

    public OauthClient() {
        this.authenticationMethods.add(ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue());
        this.authenticationMethods.add(ClientAuthenticationMethod.CLIENT_SECRET_POST.getValue());
        this.authenticationMethods.add(ClientAuthenticationMethod.CLIENT_SECRET_JWT.getValue());
        this.authenticationMethods.add(ClientAuthenticationMethod.PRIVATE_KEY_JWT.getValue());
        this.authenticationMethods.add(ClientAuthenticationMethod.NONE.getValue());


        this.grantTypes.add(AuthorizationGrantType.AUTHORIZATION_CODE.getValue());
        this.grantTypes.add(AuthorizationGrantType.REFRESH_TOKEN.getValue());
        this.grantTypes.add(AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
        this.grantTypes.add(AuthorizationGrantType.PASSWORD.getValue());
        this.grantTypes.add(AuthorizationGrantType.JWT_BEARER.getValue());
        this.grantTypes.add(AuthorizationGrantType.DEVICE_CODE.getValue());

        this.availableScopes.add(OidcScopes.OPENID);
        this.availableScopes.add(OidcScopes.PROFILE);
        this.availableScopes.add(OidcScopes.EMAIL);
        this.availableScopes.add(OidcScopes.ADDRESS);
        this.availableScopes.add(OidcScopes.PHONE);

        tokenSettings.setAccessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED);
        tokenSettings.setIdTokenSignatureAlgorithm(SignatureAlgorithm.RS256);

    }
    public List<String> getGrantTypes() {
        return this.grantTypes;
    }
    public void setClientAuthenticationMethods(List<String> clientAuthenticationMethods) {
        this.clientAuthenticationMethods = clientAuthenticationMethods;
    }
    public boolean contains(String value) {
        return clientAuthenticationMethods.contains(value);
    }
    public List<String> getClientAuthenticationMethods() {
        return this.clientAuthenticationMethods;
    }

    public void setAuthenticationMethods(List<String> authenticationMethods) {
        this.authenticationMethods = authenticationMethods;
    }
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientIdIssuedAt() {
        return clientIdIssuedAt;
    }

    public void setClientIdIssuedAt(String clientIdIssuedAt) {
        this.clientIdIssuedAt = clientIdIssuedAt;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getClientSecretExpiresAt() {
        return clientSecretExpiresAt;
    }

    public void setClientSecretExpiresAt(String clientSecretExpiresAt) {
        this.clientSecretExpiresAt = clientSecretExpiresAt;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }


    public List<String> getAuthorizationGrantTypes() {
        return authorizationGrantTypes;
    }

    public void setAuthorizationGrantTypes(List<String> authorizationGrantTypes) {
        this.authorizationGrantTypes = authorizationGrantTypes;
    }

    public String getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(String redirectUris) {
        this.redirectUris = redirectUris;
    }

    public String getPostLogoutRedirectUris() {
        return postLogoutRedirectUris;
    }

    public void setPostLogoutRedirectUris(String postLogoutRedirectUris) {
        this.postLogoutRedirectUris = postLogoutRedirectUris;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public String getFullClientId() {
        setFullClientId();
        return this.fullClientId;
    }

    private void setFullClientId(){
        if (this.clientIdUuid != null) {
            this.fullClientId = this.clientIdUuid + "." + clientId;
        }
        else {
            this.fullClientId = "." + clientId;
        }
    }

    private String toClientAuthenticationMethods(List<String> list) {
        StringBuilder stringBuilder = new StringBuilder();
        //list.remove("");

        for(String s: list) {
            if (!stringBuilder.isEmpty()) {
                stringBuilder.append(",");
            }

            switch (s) {
                case "CLIENT_SECRET_BASIC" -> stringBuilder.append(ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue());
                case "CLIENT_SECRET_POST" -> stringBuilder.append(ClientAuthenticationMethod.CLIENT_SECRET_POST.getValue());
                case "CLIENT_SECRET_JWT" -> stringBuilder.append(ClientAuthenticationMethod.CLIENT_SECRET_JWT.getValue());
                case "PRIVATE_KEY_JWT" -> stringBuilder.append(ClientAuthenticationMethod.PRIVATE_KEY_JWT.getValue());
                case "NONE" -> stringBuilder.append(ClientAuthenticationMethod.NONE.getValue());
                default -> LOG.error("invalid authenticationMethod: {}", s);
            }
        }
        return stringBuilder.toString();
    }

    public UUID getClientIdUuid() {
        return clientIdUuid;
    }



    public void setClientIdUuid(UUID clientIdUuid) {
        this.clientIdUuid = clientIdUuid;
    }

    private String toAuthorizationGrantTypes(List<String> list) {
        StringBuilder stringBuilder = new StringBuilder();
        LOG.info("list {}", list);
        list.remove(""); //remove empty space items from list

        for(String s: list) {
            if (!stringBuilder.isEmpty()) {
                stringBuilder.append(",");
            }
            switch (s) {
                case "AUTHORIZATION_CODE" -> stringBuilder.append(AuthorizationGrantType.AUTHORIZATION_CODE.getValue());
                case "REFRESH_TOKEN" -> stringBuilder.append(AuthorizationGrantType.REFRESH_TOKEN.getValue());
                case "CLIENT_CREDENTIALS" -> stringBuilder.append(AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
                case "PASSWORD" -> stringBuilder.append(AuthorizationGrantType.PASSWORD.getValue());
                case "JWT_BEARER" -> stringBuilder.append(AuthorizationGrantType.JWT_BEARER.getValue());
                case "DEVICE_CODE" -> stringBuilder.append(AuthorizationGrantType.DEVICE_CODE.getValue());
                default -> LOG.error("invalid AuthorizationGrantType {}", s);
            }
        }
        return stringBuilder.toString();
    }
    private String toOidcScopes(List<String> list) {
        StringBuilder stringBuilder = new StringBuilder();
        list.remove("");

        for(String s: list) {
            if (!stringBuilder.isEmpty()) {
                stringBuilder.append(",");
            }
            switch (s) {
                case "OPENID" -> stringBuilder.append(OidcScopes.OPENID);
                case "PROFILE" -> stringBuilder.append(OidcScopes.PROFILE);
                case "EMAIL" -> stringBuilder.append(OidcScopes.EMAIL);
                case "ADDRESS" -> stringBuilder.append(OidcScopes.ADDRESS);
                case "PHONE" -> stringBuilder.append(OidcScopes.PHONE);
                default -> LOG.error("invalid scope {}", s);
            }
        }
        return stringBuilder.toString();
    }


    /* public Map<String,Boolean> getClientSettings() {
        return clientSettings;
    }

    public void setClientSettings(Map<String, Boolean> clientSettings) {
        this.clientSettings = clientSettings;
    }*/
/*    public String getClientSettings() {
        return this.clientSettings;
    }

    public void setClientSettings(String clientSettings) {
        this.clientSettings = clientSettings;
    }*/

    public List<String> getAvailableScopes() {
        return this.availableScopes;
    }
    public ClientSettings getClientSettings() {
        return clientSettings;
    }

    public void setClientSettings(ClientSettings clientSettings) {
        this.clientSettings = clientSettings;
    }

    public TokenSettings getTokenSettings() {
        return tokenSettings;
    }

    public void setTokenSettings(TokenSettings tokenSettings) {
        this.tokenSettings = tokenSettings;
    }
    public List<String> getAuthenticationMethods() {
        return authenticationMethods;
    }

    public OidcScopes getOidcScopes() {
        return oidcScopes;
    }

    public boolean isMediateToken() {
        return this.mediateToken;
    }

    public void setMediateToken(boolean mediateToken) {
        this.mediateToken = mediateToken;
    }

    public void setCustomScopes(String customScopes) {
        this.customScopes = customScopes;
    }

    public String getCustomScopes() {
        return this.customScopes;
    }

    @Override
    public String toString() {
        return "OauthClient{" +
                "id='" + id + '\'' +
                ", clientId='" + clientId + '\'' +
                ", clientIdIssuedAt='" + clientIdIssuedAt + '\'' +
                ", clientSecret='" + clientSecret + '\'' +
                ", clientSecretExpiresAt='" + clientSecretExpiresAt + '\'' +
                ", clientName='" + clientName + '\'' +
                ", clientAuthenticationMethods=" + clientAuthenticationMethods +
                ", authorizationGrantTypes=" + authorizationGrantTypes +
                ", authenticationMethods=" + authenticationMethods +
                ", grantTypes=" + grantTypes +
                ", oidcScopes=" + oidcScopes +
                ", redirectUris='" + redirectUris + '\'' +
                ", postLogoutRedirectUris='" + postLogoutRedirectUris + '\'' +
                ", scopes=" + scopes +
                ", customScopes='" + customScopes + '\'' +
                ", clientSettings=" + clientSettings +
                ", tokenSettings=" + tokenSettings +
                ", mediateToken=" + mediateToken +
                ", availableScopes=" + availableScopes +
                '}';
    }
   /*  public Map<String, Object> getMap() {
        LOG.info("get map");
        Map<String, Object> map = new HashMap<>(Map.of("clientId", clientId, "clientIdIssuedAt", clientIdIssuedAt,
                "clientSecret", clientSecret, "clientSecretExpiresAt", clientSecretExpiresAt,
                "clientName", clientName,
                 "redirectUris", redirectUris,
                "postLogoutRedirectUris", postLogoutRedirectUris
        ));
        map.put("clientAuthenticationMethods", toClientAuthenticationMethods(clientAuthenticationMethods));
        map.put("authorizationGrantTypes", toAuthorizationGrantTypes(authorizationGrantTypes));
        map.put("scopes", toOidcScopes(scopes) + customScopes);

        if (clientSettings.isEmpty()) {
            clientSettings = new HashMap<>().toString();
        }
        LOG.info("clientSettings: {}", clientSettings);
        if (!clientSettings.isEmpty()) {
            String[] clientSettingsArray = clientSettings.split(",");
            Map<String, String> clientSettingsMap = new HashMap<>();

            for (String cs : clientSettingsArray) {
                String[] keyValueArray = cs.split("=");
                clientSettingsMap.put(keyValueArray[0], keyValueArray[1]);
            }
            LOG.info("clientSettingsMaps: {}", clientSettingsMap);
            map.put("clientSettings", clientSettingsMap);
        }
        else {
            LOG.info("clientSettings empty");
        }

        map.put("tokenSettings", tokenSettings);
        map.put("mediateToken", mediateToken);
        return map;
    }*/

    /**
     * {
     * redirectUris=http://127.0.0.1:8080/authorized,http://127.0.0.1:8080/login/oauth2/code/my-client-oidc,
     * scopes=openid,profile,message.read,message.write,
     * clientId=4f6c9c8c-2637-4f7c-b90d-f127fb4fc39f,
     * clientSecret={noop}secret,
     * clientSettings={"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":"false","settings.client.require-authorization-consent":"true"},
     * clientName=Blog Application,
     * id=c9e2fdf6-7994-4448-b0d0-1f943395bbe3,
     * clientAuthenticationMethods=client_secret_jwt,client_secret_basic,
     * authorizationGrantTypes=refresh_token,client_credentials,authorization_code,
     * tokenSettings={"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",300.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",3600.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000],"settings.token.device-code-time-to-live":["java.time.Duration",300.000000000]}}
     *
     * @return
     */
    /*public static OauthClient getFromMap(Map<String, Object> map) {
        OauthClient oauthClient = new OauthClient();
        oauthClient.setId(map.get("id").toString());
        oauthClient.setClientId(map.get("clientId").toString());
        if (map.get("clientName") != null) {
            oauthClient.setClientName(map.get("clientName").toString());
        }
        if (map.get("clientSecret") != null) {
            oauthClient.setClientSecret(map.get("clientSecret").toString());
        }
        if (map.get("clientSecretExpiresAt") != null) {
            oauthClient.setClientSecretExpiresAt(map.get("clientSecretExpiresAt").toString());
        }
        if (map.get("redirectUris") != null) {
            oauthClient.setRedirectUris(map.get("redirectUris").toString());
        }
        if (map.get("postLogoutRedirectUris") != null) {
            oauthClient.setPostLogoutRedirectUris(map.get("postLogoutRedirectUris").toString());
        }

        if (map.get("scopes") != null) {
            List<String> list = Arrays.stream(map.get("scopes").toString().split(",")).toList();
            oauthClient.setScopes(list);
        }

        if (map.get("clientAuthenticationMethods") != null) {
            List<String> list = Arrays.stream(map.get("clientAuthenticationMethods").toString().split(",")).toList();
            oauthClient.setClientAuthenticationMethods(list);
        }
        if (map.get("authorizationGrantTypes") != null) {
            List<String> list = Arrays.stream(map.get("authorizationGrantTypes").toString().split(",")).toList();
            oauthClient.setAuthorizationGrantTypes(list);
        }

        if (map.get("clientSettings") != null) {
            String clientSettings = map.get("clientSettings").toString();
            clientSettings = clientSettings.replace("{\"@class\":\"java.util.Collections$UnmodifiableMap\"", "");
            clientSettings = clientSettings.replace("}", "");

            List<String> list = Arrays.stream(clientSettings.split(",")).toList();

            StringBuilder stringBuilder = getKeyValue(list);
            oauthClient.setClientSettings(stringBuilder.toString());

        }

        if (map.get("tokenSettings") != null) {
            String clientSettings = map.get("tokenSettings").toString();
            clientSettings = clientSettings.replace("{\"@class\":\"java.util.Collections$UnmodifiableMap\"", "");
            clientSettings = clientSettings.replace("}", "");

            List<String> list = Arrays.stream(clientSettings.split(",")).toList();

            StringBuilder stringBuilder = getKeyValue(list);
            oauthClient.setTokenSettings(stringBuilder.toString());
        }
        return oauthClient;
    }*/

    public static String toCsvString(Set<String> set) {
        StringBuilder stringBuilder = new StringBuilder();
        for(String s: set) {
            if (!stringBuilder.isEmpty()) {
                stringBuilder.append(",");
            }
            stringBuilder.append(s);
        }
        return stringBuilder.toString();
    }

    public static OauthClient getFromRegisteredClient(RegisteredClient registeredClient) {
        OauthClient oauthClient = new OauthClient();
        oauthClient.setId(registeredClient.getId());
        oauthClient.setClientId(registeredClient.getClientId());
        oauthClient.setMediateToken(registeredClient.isMediateToken());

        oauthClient.setClientName(registeredClient.getClientName());
        oauthClient.setClientSecret(registeredClient.getClientSecret());
        if (registeredClient.getClientSecretExpiresAt() != null) {
            oauthClient.setClientSecretExpiresAt(registeredClient.getClientSecretExpiresAt().toString());
        }
        oauthClient.setRedirectUris(toCsvString(registeredClient.getRedirectUris()));
        oauthClient.setPostLogoutRedirectUris(toCsvString(registeredClient.getPostLogoutRedirectUris()));

        List<String> list = new ArrayList<>(registeredClient.getScopes());
        oauthClient.setScopes(list);

        List<String> chosen = new ArrayList<>(registeredClient.getScopes());
        LOG.info("chosen scope: {}", chosen);
        List<String> standardList = new ArrayList<>(oauthClient.getAvailableScopes());
        LOG.info("standard scopes available: {}", standardList);
        chosen.removeAll(standardList);

        StringBuilder customScopeStringBuilder = new StringBuilder();

        for (String s: chosen) {
            LOG.info("s: {}", s);
            if (!s.isEmpty()) {
                if (!customScopeStringBuilder.isEmpty()) {
                    customScopeStringBuilder.append(",");
                }
                customScopeStringBuilder.append(s);
            }
            else {
                LOG.info("s is empty: '{}'", s);
            }
        }

        LOG.info("stringBUilder customScope: {}", customScopeStringBuilder.toString());
        oauthClient.setCustomScopes(customScopeStringBuilder.toString());

        //String customScopes = chosen.stream().map(s -> s + ",").collect(Collectors.joining(","));
        //oauthClient.setCustomScopes(customScopes);
        //LOG.info("customScope: {}", customScopes);


        if (!registeredClient.getClientAuthenticationMethods().isEmpty()) {
            //List<String> clientAuthenticationMethods = registeredClient.getClientAuthenticationMethods().stream().map(ClientAuthenticationMethod::getValue::).toList();
            for(ClientAuthenticationMethod cam: registeredClient.getClientAuthenticationMethods()) {
                //LOG.info("clientAuthenticationMethods: {}", clientAuthenticationMethods);
                //oauthClient.setClientAuthenticationMethods(clientAuthenticationMethods);
                oauthClient.getClientAuthenticationMethods().add(cam.getValue().toUpperCase());
                LOG.info("cam: {}", oauthClient.getClientAuthenticationMethods());
            }
        }
        if (!registeredClient.getAuthorizationGrantTypes().isEmpty()) {
            List<String> authorizationGrantTypeList = registeredClient.getAuthorizationGrantTypes().stream().map(AuthorizationGrantType::getValue).toList();
            LOG.info("authorizationGrantTypeList: {}", authorizationGrantTypeList);
            oauthClient.setAuthorizationGrantTypes(authorizationGrantTypeList);
        }
        oauthClient.clientSettings = new ClientSettings();

        if (registeredClient.getClientSettings().isRequireProofKey()) {
            oauthClient.clientSettings.setRequireProofKey(true);
        }
        if (registeredClient.getClientSettings().isRequireAuthorizationConsent()) {
            oauthClient.clientSettings.setRequireAuthorizationConsent(true);
        }
        if (registeredClient.getClientSettings().getJwkSetUrl()!= null && !registeredClient.getClientSettings().getJwkSetUrl().isEmpty()) {
            oauthClient.clientSettings.setJwkSetUrl(registeredClient.getClientSettings().getJwkSetUrl());
        }

        oauthClient.tokenSettings.setReuseRefreshTokens(registeredClient.getTokenSettings().isReuseRefreshTokens());
        oauthClient.tokenSettings.setAuthorizationCodeTimeToLive(registeredClient.getTokenSettings().getAuthorizationCodeTimeToLive().getSeconds());
        oauthClient.tokenSettings.setAccessTokenTimeToLive(registeredClient.getTokenSettings().getAccessTokenTimeToLive().getSeconds());
        oauthClient.tokenSettings.setAccessTokenFormat(registeredClient.getTokenSettings().getAccessTokenFormat());
        oauthClient.tokenSettings.setDeviceCodeTimeToLive(registeredClient.getTokenSettings().getDeviceCodeTimeToLive().getSeconds());
        oauthClient.tokenSettings.setReuseRefreshTokens(registeredClient.getTokenSettings().isReuseRefreshTokens());
        oauthClient.tokenSettings.setRefreshTokenTimeToLive(registeredClient.getTokenSettings().getRefreshTokenTimeToLive().getSeconds());
        oauthClient.tokenSettings.setIdTokenSignatureAlgorithm(registeredClient.getTokenSettings().getIdTokenSignatureAlgorithm());

        return oauthClient;
    }

    private static StringBuilder getKeyValue(List<String> list) {
        StringBuilder stringBuilder = new StringBuilder();
        for(String item: list) {
            if (!stringBuilder.isEmpty()) {
                if (!item.isEmpty()) {
                    stringBuilder.append(",");
                }
            }
            LOG.info("item: {}", item);
            String clientSetting = item;
            clientSetting = clientSetting.replace("\"", "");
            clientSetting = clientSetting.replace(":", "=");
            stringBuilder.append(clientSetting);

        }
        return stringBuilder;
    }

    public RegisteredClient getInitialRegisteredClient() {
        RegisteredClient.Builder registeredClientBuilder = RegisteredClient.withId(id).clientId(clientId)
                .clientSecret(clientSecret);

        for(String s: clientAuthenticationMethods) {
            switch (s) {
                case "CLIENT_SECRET_BASIC" -> registeredClientBuilder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
                case "CLIENT_SECRET_POST" -> registeredClientBuilder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST);
                case "CLIENT_SECRET_JWT" -> registeredClientBuilder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_JWT);
                case "PRIVATE_KEY_JWT" -> registeredClientBuilder.clientAuthenticationMethod(ClientAuthenticationMethod.PRIVATE_KEY_JWT);
                case "NONE" -> registeredClientBuilder.clientAuthenticationMethod(ClientAuthenticationMethod.NONE);
                default -> LOG.error("invalid authenticationMethod: {}", s);
            }
        }
        for(String s: authorizationGrantTypes) {
            switch (s) {
                case "AUTHORIZATION_CODE" -> registeredClientBuilder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
                case "REFRESH_TOKEN" -> registeredClientBuilder.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN);
                case "CLIENT_CREDENTIALS" -> registeredClientBuilder.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS);
                case "PASSWORD" -> registeredClientBuilder.authorizationGrantType(AuthorizationGrantType.PASSWORD);
                case "JWT_BEARER" -> registeredClientBuilder.authorizationGrantType(AuthorizationGrantType.JWT_BEARER);
                case "DEVICE_CODE" -> registeredClientBuilder.authorizationGrantType(AuthorizationGrantType.DEVICE_CODE);
                default -> LOG.error("invalid AuthorizationGrantType {}", s);
            }
        }
        for(String s: scopes) {
            switch (s) {
                case "OPENID" -> registeredClientBuilder.scope(OidcScopes.OPENID);
                case "PROFILE" -> registeredClientBuilder.scope(OidcScopes.PROFILE);
                case "EMAIL" -> registeredClientBuilder.scope(OidcScopes.EMAIL);
                case "ADDRESS" -> registeredClientBuilder.scope(OidcScopes.ADDRESS);
                case "PHONE" -> registeredClientBuilder.scope(OidcScopes.PHONE);
                default -> LOG.error("invalid scope {}", s);
            }
        }

        if (customScopes != null && !customScopes.isEmpty()) {
            List<String> customScopeList = Arrays.stream(customScopes.split(",")).toList();
            customScopeList.stream().map(String::trim).filter(s -> !s.isEmpty()).forEach(registeredClientBuilder::scope);
            LOG.info("added custom scope {}", customScopeList);
        }

        if (redirectUris != null ) {
            registeredClientBuilder.redirectUri(redirectUris);
        }
        if (postLogoutRedirectUris != null) {
            registeredClientBuilder.postLogoutRedirectUri(postLogoutRedirectUris);
        }
        registeredClientBuilder.mediateToken(true);

        return registeredClientBuilder.build();
    }

    public RegisteredClient getRegisteredClient() {
        if (id == null || id.isEmpty()) {
           // id = UUID.randomUUID().toString();

            LOG.info("this RegisteredClient has not been created yet");
            LOG.info("copy clientIdUuid and append with .clientId");
            clientId = clientIdUuid + "-" + clientId;
            LOG.info("id is null, prepend clientIdUuid to clientIdString: {}", clientId);
        }
        else {
            LOG.info("id is not empty: '{}'", id);
        }

        LOG.info("clientId: {}", clientId);

        RegisteredClient.Builder registeredClientBuilder = RegisteredClient.withId(id)
                .clientId(clientId)
                .clientSecret(clientSecret).clientName(clientId);

        for(String s: clientAuthenticationMethods) {
            switch (s) {
                case "CLIENT_SECRET_BASIC" -> registeredClientBuilder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
                case "CLIENT_SECRET_POST" -> registeredClientBuilder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST);
                case "CLIENT_SECRET_JWT" -> registeredClientBuilder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_JWT);
                case "PRIVATE_KEY_JWT" -> registeredClientBuilder.clientAuthenticationMethod(ClientAuthenticationMethod.PRIVATE_KEY_JWT);
                case "NONE" -> registeredClientBuilder.clientAuthenticationMethod(ClientAuthenticationMethod.NONE);
                default -> LOG.error("invalid authenticationMethod: {}", s);
            }
        }

        for(String s: authorizationGrantTypes) {
            switch (s) {
                case "AUTHORIZATION_CODE" -> registeredClientBuilder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
                case "REFRESH_TOKEN" -> registeredClientBuilder.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN);
                case "CLIENT_CREDENTIALS" -> registeredClientBuilder.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS);
                case "PASSWORD" -> registeredClientBuilder.authorizationGrantType(AuthorizationGrantType.PASSWORD);
                case "JWT_BEARER" -> registeredClientBuilder.authorizationGrantType(AuthorizationGrantType.JWT_BEARER);
                case "DEVICE_CODE" -> registeredClientBuilder.authorizationGrantType(AuthorizationGrantType.DEVICE_CODE);
                default -> LOG.error("invalid AuthorizationGrantType {}", s);
            }
        }
        for(String s: scopes) {
            switch (s) {
                case "OPENID" -> registeredClientBuilder.scope(OidcScopes.OPENID);
                case "PROFILE" -> registeredClientBuilder.scope(OidcScopes.PROFILE);
                case "EMAIL" -> registeredClientBuilder.scope(OidcScopes.EMAIL);
                case "ADDRESS" -> registeredClientBuilder.scope(OidcScopes.ADDRESS);
                case "PHONE" -> registeredClientBuilder.scope(OidcScopes.PHONE);
                default -> LOG.error("invalid scope {}", s);
            }
        }

        if (customScopes != null && !customScopes.isEmpty()) {
            List<String> customScopeList = Arrays.stream(customScopes.split(",")).toList();
            customScopeList.stream().map(String::trim).filter(s -> !s.isEmpty()).forEach(registeredClientBuilder::scope);
            LOG.info("added custom scope {}", customScopeList);
        }

        if (redirectUris != null && !redirectUris.trim().isEmpty()) {
            Arrays.stream(redirectUris.split(",")).forEach(registeredClientBuilder::redirectUri);
        }
        if (postLogoutRedirectUris != null && !postLogoutRedirectUris.trim().isEmpty()) {
            Arrays.stream(postLogoutRedirectUris.split(",")).forEach(registeredClientBuilder::postLogoutRedirectUri);
        }

        registeredClientBuilder.mediateToken(mediateToken);
        me.sonam.authzmanager.controller.admin.oauth2.ClientSettings.Builder clientSettings1Builder =
                me.sonam.authzmanager.controller.admin.oauth2.ClientSettings.builder();

        if (clientSettings != null) {
            clientSettings1Builder.requireAuthorizationConsent(clientSettings.isRequireAuthorizationConsent());
            clientSettings1Builder.requireProofKey(clientSettings.isRequireProofKey());

            if (clientSettings.getJwkSetUrl() != null && !clientSettings.getJwkSetUrl().isEmpty()) {
                clientSettings1Builder.jwkSetUrl(clientSettings.jwkSetUrl);
            }
            me.sonam.authzmanager.controller.admin.oauth2.ClientSettings clientSettings1 = clientSettings1Builder.build();
            registeredClientBuilder.clientSettings(clientSettings1);
            LOG.info("set clientSettings in registeredClientBuilder");
        }

        me.sonam.authzmanager.controller.admin.oauth2.TokenSettings.Builder tokenSettingsBuilder =
                me.sonam.authzmanager.controller.admin.oauth2.TokenSettings.builder();
        if (tokenSettings != null) {
            tokenSettingsBuilder.reuseRefreshTokens(tokenSettings.isReuseRefreshTokens());
            tokenSettingsBuilder.authorizationCodeTimeToLive(Duration.ofSeconds(tokenSettings.getAuthorizationCodeTimeToLive()));
            tokenSettingsBuilder.accessTokenFormat(tokenSettings.getAccessTokenFormat());
            tokenSettingsBuilder.deviceCodeTimeToLive(Duration.ofSeconds(tokenSettings.getDeviceCodeTimeToLive()));
            tokenSettingsBuilder.reuseRefreshTokens(tokenSettings.isReuseRefreshTokens());

            tokenSettingsBuilder.refreshTokenTimeToLive(Duration.ofSeconds(tokenSettings.getRefreshTokenTimeToLive()));
            tokenSettingsBuilder.idTokenSignatureAlgorithm(tokenSettings.getIdTokenSignatureAlgorithm());

            registeredClientBuilder.tokenSettings(tokenSettingsBuilder.build());
        }

        RegisteredClient registeredClient = registeredClientBuilder.build();

        LOG.info("registeredClient: {}", registeredClient);

        return registeredClient;

    }


}