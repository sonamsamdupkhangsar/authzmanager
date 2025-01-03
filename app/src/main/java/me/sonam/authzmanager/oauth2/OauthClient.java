package me.sonam.authzmanager.oauth2;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
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
    private String newClientSecret;
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

    private ClientSettings clientSettings = new ClientSettings();

    private TokenSettings tokenSettings = new TokenSettings();
    public static class ClientSettings {
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

    public String getNewClientSecret() {
        return newClientSecret;
    }

    public void setNewClientSecret(String newClientSecret) {
        this.newClientSecret = newClientSecret;
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
    private List<String> availableScopes = new ArrayList<>();

    public OauthClient() {
        this.authenticationMethods.add(ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue());
        this.authenticationMethods.add(ClientAuthenticationMethod.CLIENT_SECRET_POST.getValue());
        this.authenticationMethods.add(ClientAuthenticationMethod.CLIENT_SECRET_JWT.getValue());
        this.authenticationMethods.add(ClientAuthenticationMethod.PRIVATE_KEY_JWT.getValue());
        this.authenticationMethods.add(ClientAuthenticationMethod.TLS_CLIENT_AUTH.getValue());
        this.authenticationMethods.add(ClientAuthenticationMethod.SELF_SIGNED_TLS_CLIENT_AUTH.getValue());
        this.authenticationMethods.add(ClientAuthenticationMethod.NONE.getValue());


        this.grantTypes.add(AuthorizationGrantType.AUTHORIZATION_CODE.getValue());
        this.grantTypes.add(AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
        this.grantTypes.add(AuthorizationGrantType.REFRESH_TOKEN.getValue());
        this.grantTypes.add(AuthorizationGrantType.JWT_BEARER.getValue());
        this.grantTypes.add(AuthorizationGrantType.DEVICE_CODE.getValue());
        this.grantTypes.add(AuthorizationGrantType.TOKEN_EXCHANGE.getValue());

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

   /* private String toClientAuthenticationMethods(List<String> list) {
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
    }*/

    public UUID getClientIdUuid() {
        return clientIdUuid;
    }



    public void setClientIdUuid(UUID clientIdUuid) {
        this.clientIdUuid = clientIdUuid;
    }

 /*   private String toAuthorizationGrantTypes(List<String> list) {
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
                case "JWT_BEARER" -> stringBuilder.append(AuthorizationGrantType.JWT_BEARER.getValue());
                case "DEVICE_CODE" -> stringBuilder.append(AuthorizationGrantType.DEVICE_CODE.getValue());
                default -> LOG.error("invalid AuthorizationGrantType {}", s);
            }
        }
        return stringBuilder.toString();
    }*/
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
                ", availableScopes=" + availableScopes +
                '}';
    }

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

        if (registeredClient.getClientIdIssuedAt() != null) {
            Instant clientIdIssuedAt = registeredClient.getClientIdIssuedAt();
            LOG.info("use localDateTime without timezone to set in calendar ui, before conversion to localTime {}", clientIdIssuedAt);
            LocalDateTime localDateTime = LocalDateTime.ofInstant(clientIdIssuedAt, ZoneId.systemDefault());

            Instant instant = localDateTime.toInstant(ZoneOffset.UTC);
            LOG.info("instant: {}, localDateTime: {}", instant, localDateTime);

            oauthClient.setClientIdIssuedAt(localDateTime.toString());
            LOG.info("oauthClient.clientIdIssuedAt: {}", oauthClient.getClientIdIssuedAt());
        }
        if (registeredClient.getClientSecretExpiresAt() != null) {
            Instant clientSecretExpiresAt = registeredClient.getClientSecretExpiresAt();
            LOG.info("use localDateTime without timezone to set in calendar ui, before conversion to localTime {}", clientSecretExpiresAt);
            LocalDateTime localDateTime = LocalDateTime.ofInstant(clientSecretExpiresAt, ZoneId.systemDefault());
            Instant instant = localDateTime.toInstant(ZoneOffset.UTC);

            LOG.info("instant: {}, localDateTime: {}", instant, localDateTime);
            oauthClient.setClientSecretExpiresAt(localDateTime.toString());
            LOG.info("oauthClient.clientSecretExpiresAt: {}", oauthClient.getClientSecretExpiresAt());
        }

        oauthClient.setClientName(registeredClient.getClientName());
        oauthClient.setClientSecret(registeredClient.getClientSecret());

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

        if (!registeredClient.getClientAuthenticationMethods().isEmpty()) {
            for(ClientAuthenticationMethod cam: registeredClient.getClientAuthenticationMethods()) {
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

   public DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm");

   private Instant getInstant(String dateString) {
       if (dateString == null || dateString.isEmpty()) {
           LOG.warn("dateString is null/empty: {}", dateString);
           return null;
       }

       try {
            Date date = formatter.parse(dateString);
            LOG.info("date: {}, dateString: {}", date, dateString);
            Instant dateInstant = date.toInstant();
            LocalDateTime localDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());

            LOG.info("dateInstant {} vs localDateTime instant {}, for dateString: {}", dateInstant, localDateTime, dateString);
            return dateInstant;
            //return localDateTime.toInstant(ZoneOffset.UTC);
       }
       catch (ParseException e) {
           LOG.error("failed to parse clientIdIssuedAt to dateformat", e);
           return null;
       }
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
                .clientId(clientId).clientSecret(clientSecret).clientName(clientId)
                .newClientSecret(newClientSecret);

        registeredClientBuilder.clientIdIssuedAt(getInstant(clientIdIssuedAt));
        registeredClientBuilder.clientSecretExpiresAt(getInstant(clientSecretExpiresAt));

        for(String s: clientAuthenticationMethods) {
            switch (s) {
                case "CLIENT_SECRET_BASIC" -> registeredClientBuilder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
                case "CLIENT_SECRET_POST" -> registeredClientBuilder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST);
                case "CLIENT_SECRET_JWT" -> registeredClientBuilder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_JWT);
                case "PRIVATE_KEY_JWT" -> registeredClientBuilder.clientAuthenticationMethod(ClientAuthenticationMethod.PRIVATE_KEY_JWT);
                case "TLS_CLIENT_AUTH" -> registeredClientBuilder.clientAuthenticationMethod(ClientAuthenticationMethod.TLS_CLIENT_AUTH);
                case "SELF_SIGNED_TLS_CLIENT_AUTH" -> registeredClientBuilder.clientAuthenticationMethod(ClientAuthenticationMethod.SELF_SIGNED_TLS_CLIENT_AUTH);
                case "NONE" -> registeredClientBuilder.clientAuthenticationMethod(ClientAuthenticationMethod.NONE);
                default -> LOG.error("invalid authenticationMethod: {}", s);
            }
        }

        for(String s: authorizationGrantTypes) {
            switch (s) {
                case "AUTHORIZATION_CODE" -> registeredClientBuilder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
                case "CLIENT_CREDENTIALS" -> registeredClientBuilder.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS);
                case "REFRESH_TOKEN" -> registeredClientBuilder.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN);
                case "DEVICE_CODE" -> registeredClientBuilder.authorizationGrantType(AuthorizationGrantType.DEVICE_CODE);
                case "JWT-BEARER" -> registeredClientBuilder.authorizationGrantType(AuthorizationGrantType.JWT_BEARER);//consistent with value jwt-bearer
                case "TOKEN-EXCHANGE" -> registeredClientBuilder.authorizationGrantType(AuthorizationGrantType.TOKEN_EXCHANGE);//consistent with value token-exchange
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

        me.sonam.authzmanager.oauth2.ClientSettings.Builder clientSettings1Builder =
                me.sonam.authzmanager.oauth2.ClientSettings.builder();

        if (clientSettings != null) {
            clientSettings1Builder.requireAuthorizationConsent(clientSettings.isRequireAuthorizationConsent());
            clientSettings1Builder.requireProofKey(clientSettings.isRequireProofKey());

            if (clientSettings.getJwkSetUrl() != null && !clientSettings.getJwkSetUrl().isEmpty()) {
                clientSettings1Builder.jwkSetUrl(clientSettings.jwkSetUrl);
            }
            me.sonam.authzmanager.oauth2.ClientSettings clientSettings1 = clientSettings1Builder.build();
            registeredClientBuilder.clientSettings(clientSettings1);
            LOG.info("set clientSettings in registeredClientBuilder");
        }

        me.sonam.authzmanager.oauth2.TokenSettings.Builder tokenSettingsBuilder =
                me.sonam.authzmanager.oauth2.TokenSettings.builder();
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