package me.sonam.authzmanager.controller.admin.oauth2;

import java.time.Instant;
import java.util.*;

public class OauthClient {
    private String id;
    private String clientId;
    private String clientIdIssuedAt;
    private String clientSecret;
    private String clientSecretExpiresAt;
    private String clientName;
    //private String clientAuthenticationMethods;
    private List<String> clientAuthenticationMethods = new ArrayList<>();
    private List<String> authorizationGrantTypes = new ArrayList<>();

    private List<String> authenticationMethods = new ArrayList<>();
    private List<String> grantTypes = new ArrayList<>();
    private OidcScopes oidcScopes;

    private String redirectUris;
    private String postLogoutRedirectUris;
    private List<String> scopes = new ArrayList<>();
    private String clientSettings;
    private String tokenSettings;

    public OauthClient() {
        this.authenticationMethods.add(ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue());
        this.authenticationMethods.add(ClientAuthenticationMethod.CLIENT_SECRET_JWT.getValue());
        this.authenticationMethods.add(ClientAuthenticationMethod.PRIVATE_KEY_JWT.getValue());
        this.authenticationMethods.add(ClientAuthenticationMethod.CLIENT_SECRET_POST.getValue());

        this.grantTypes.add(AuthorizationGrantType.AUTHORIZATION_CODE.getValue());
        this.grantTypes.add(AuthorizationGrantType.REFRESH_TOKEN.getValue());
        this.grantTypes.add(AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
        this.grantTypes.add(AuthorizationGrantType.PASSWORD.getValue());
        this.grantTypes.add(AuthorizationGrantType.JWT_BEARER.getValue());
        this.grantTypes.add(AuthorizationGrantType.DEVICE_CODE.getValue());

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

    public String getClientSettings() {
        return clientSettings;
    }

    public void setClientSettings(String clientSettings) {
        this.clientSettings = clientSettings;
    }

    public String getTokenSettings() {
        return tokenSettings;
    }

    public void setTokenSettings(String tokenSettings) {
        this.tokenSettings = tokenSettings;
    }
    public List<String> getAuthenticationMethods() {
        return authenticationMethods;
    }

    public OidcScopes getOidcScopes() {
        return oidcScopes;
    }

    @Override
    public String toString() {
        return "OauthClient{" +
                "id='" + id + '\'' +
                ", clientId='" + clientId + '\'' +
                ", clientIdIssuedAt=" + clientIdIssuedAt +
                ", clientSecret='" + clientSecret + '\'' +
                ", clientSecretExpiresAt=" + clientSecretExpiresAt +
                ", clientName='" + clientName + '\'' +
                ", clientAuthenticationMethods=" + clientAuthenticationMethods +
                ", authorizationGrantTypes=" + authorizationGrantTypes +
                ", redirectUris=" + redirectUris +
                ", postLogoutRedirectUris=" + postLogoutRedirectUris +
                ", scopes=" + scopes +
                ", clientSettings=" + clientSettings +
                ", tokenSettings=" + tokenSettings +
                ", assignedAuthenticationMethods="+authenticationMethods +
                '}';
    }
}