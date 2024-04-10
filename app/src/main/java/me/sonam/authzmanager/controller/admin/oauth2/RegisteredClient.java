package me.sonam.authzmanager.controller.admin.oauth2;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

public class RegisteredClient implements Serializable {
    private static final long serialVersionUID;
    private String id;
    private String clientId;
    private Instant clientIdIssuedAt;
    private String clientSecret;
    private Instant clientSecretExpiresAt;
    private String clientName;
    private Set<ClientAuthenticationMethod> clientAuthenticationMethods;
    private Set<AuthorizationGrantType> authorizationGrantTypes;
    private Set<String> redirectUris;
    private Set<String> postLogoutRedirectUris;
    private Set<String> scopes;
    private ClientSettings clientSettings;
    private TokenSettings tokenSettings;
    private boolean mediateToken;

    protected RegisteredClient() {
    }

    public void setMediateToken(boolean value) {
        this.mediateToken = value;
    }

    public String getId() {
        return this.id;
    }

    public String getClientId() {
        return this.clientId;
    }

    public boolean isMediateToken() {
        return mediateToken;
    }

    @Nullable
    public Instant getClientIdIssuedAt() {
        return this.clientIdIssuedAt;
    }

    @Nullable
    public String getClientSecret() {
        return this.clientSecret;
    }

    @Nullable
    public Instant getClientSecretExpiresAt() {
        return this.clientSecretExpiresAt;
    }

    public String getClientName() {
        return this.clientName;
    }

    public Set<ClientAuthenticationMethod> getClientAuthenticationMethods() {
        return this.clientAuthenticationMethods;
    }

    public Set<AuthorizationGrantType> getAuthorizationGrantTypes() {
        return this.authorizationGrantTypes;
    }

    public Set<String> getRedirectUris() {
        return this.redirectUris;
    }

    public Set<String> getPostLogoutRedirectUris() {
        return this.postLogoutRedirectUris;
    }

    public Set<String> getScopes() {
        return this.scopes;
    }

    public ClientSettings getClientSettings() {
        return this.clientSettings;
    }

    public TokenSettings getTokenSettings() {
        return this.tokenSettings;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj != null && this.getClass() == obj.getClass()) {
            RegisteredClient that = (RegisteredClient)obj;
            return Objects.equals(this.id, that.id) && Objects.equals(this.clientId, that.clientId) && Objects.equals(this.clientIdIssuedAt, that.clientIdIssuedAt) && Objects.equals(this.clientSecret, that.clientSecret) && Objects.equals(this.clientSecretExpiresAt, that.clientSecretExpiresAt) && Objects.equals(this.clientName, that.clientName) && Objects.equals(this.clientAuthenticationMethods, that.clientAuthenticationMethods) && Objects.equals(this.authorizationGrantTypes, that.authorizationGrantTypes) && Objects.equals(this.redirectUris, that.redirectUris) && Objects.equals(this.postLogoutRedirectUris, that.postLogoutRedirectUris) && Objects.equals(this.scopes, that.scopes) && Objects.equals(this.clientSettings, that.clientSettings) && Objects.equals(this.tokenSettings, that.tokenSettings);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.id, this.clientId, this.clientIdIssuedAt, this.clientSecret, this.clientSecretExpiresAt, this.clientName, this.clientAuthenticationMethods, this.authorizationGrantTypes, this.redirectUris, this.postLogoutRedirectUris, this.scopes, this.clientSettings, this.tokenSettings});
    }

    public String toString() {
        return "RegisteredClient {id='" + this.id + "', clientId='" + this.clientId + "', clientName='" + this.clientName + "', clientAuthenticationMethods=" + this.clientAuthenticationMethods + ", authorizationGrantTypes=" + this.authorizationGrantTypes + ", redirectUris=" + this.redirectUris + ", postLogoutRedirectUris=" + this.postLogoutRedirectUris + ", scopes=" + this.scopes + ", clientSettings=" + this.clientSettings + ", tokenSettings=" + this.tokenSettings + "}";
    }

    public static Builder withId(String id) {
        //Assert.hasText(id, "id cannot be empty");
        return new Builder(id);
    }

    public static Builder from(RegisteredClient registeredClient) {
        Assert.notNull(registeredClient, "registeredClient cannot be null");
        return new Builder(registeredClient);
    }

    static {
        serialVersionUID = SpringAuthorizationServerVersion.SERIAL_VERSION_UID;
    }

    public static class Builder implements Serializable {
        private static final long serialVersionUID;
        private String id;
        private String clientId;
        private Instant clientIdIssuedAt;
        private String clientSecret;
        private Instant clientSecretExpiresAt;
        private String clientName;
        private final Set<ClientAuthenticationMethod> clientAuthenticationMethods = new HashSet();
        private final Set<AuthorizationGrantType> authorizationGrantTypes = new HashSet();
        private final Set<String> redirectUris = new HashSet();
        private final Set<String> postLogoutRedirectUris = new HashSet();
        private final Set<String> scopes = new HashSet();
        private ClientSettings clientSettings;
        private TokenSettings tokenSettings;
        private boolean mediateToken;

        protected Builder(String id) {
            this.id = id;
        }

        protected Builder(RegisteredClient registeredClient) {
            this.id = registeredClient.getId();
            this.clientId = registeredClient.getClientId();
            this.clientIdIssuedAt = registeredClient.getClientIdIssuedAt();
            this.clientSecret = registeredClient.getClientSecret();
            this.clientSecretExpiresAt = registeredClient.getClientSecretExpiresAt();
            this.clientName = registeredClient.getClientName();
            if (!CollectionUtils.isEmpty(registeredClient.getClientAuthenticationMethods())) {
                this.clientAuthenticationMethods.addAll(registeredClient.getClientAuthenticationMethods());
            }

            if (!CollectionUtils.isEmpty(registeredClient.getAuthorizationGrantTypes())) {
                this.authorizationGrantTypes.addAll(registeredClient.getAuthorizationGrantTypes());
            }

            if (!CollectionUtils.isEmpty(registeredClient.getRedirectUris())) {
                this.redirectUris.addAll(registeredClient.getRedirectUris());
            }

            if (!CollectionUtils.isEmpty(registeredClient.getPostLogoutRedirectUris())) {
                this.postLogoutRedirectUris.addAll(registeredClient.getPostLogoutRedirectUris());
            }

            if (!CollectionUtils.isEmpty(registeredClient.getScopes())) {
                this.scopes.addAll(registeredClient.getScopes());
            }

            this.clientSettings = ClientSettings.withSettings(registeredClient.getClientSettings().getSettings()).build();
            this.tokenSettings = TokenSettings.withSettings(registeredClient.getTokenSettings().getSettings()).build();
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder clientIdIssuedAt(Instant clientIdIssuedAt) {
            this.clientIdIssuedAt = clientIdIssuedAt;
            return this;
        }

        public Builder mediateToken(boolean value) {
            this.mediateToken = value;
            return this;
        }

        public Builder clientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }

        public Builder clientSecretExpiresAt(Instant clientSecretExpiresAt) {
            this.clientSecretExpiresAt = clientSecretExpiresAt;
            return this;
        }

        public Builder clientName(String clientName) {
            this.clientName = clientName;
            return this;
        }

        public Builder clientAuthenticationMethod(ClientAuthenticationMethod clientAuthenticationMethod) {
            this.clientAuthenticationMethods.add(clientAuthenticationMethod);
            return this;
        }

        public Builder clientAuthenticationMethods(Consumer<Set<ClientAuthenticationMethod>> clientAuthenticationMethodsConsumer) {
            clientAuthenticationMethodsConsumer.accept(this.clientAuthenticationMethods);
            return this;
        }

        public Builder authorizationGrantType(AuthorizationGrantType authorizationGrantType) {
            this.authorizationGrantTypes.add(authorizationGrantType);
            return this;
        }

        public Builder authorizationGrantTypes(Consumer<Set<AuthorizationGrantType>> authorizationGrantTypesConsumer) {
            authorizationGrantTypesConsumer.accept(this.authorizationGrantTypes);
            return this;
        }

        public Builder redirectUri(String redirectUri) {
            this.redirectUris.add(redirectUri);
            return this;
        }

        public Builder redirectUris(Consumer<Set<String>> redirectUrisConsumer) {
            redirectUrisConsumer.accept(this.redirectUris);
            return this;
        }

        public Builder postLogoutRedirectUri(String postLogoutRedirectUri) {
            this.postLogoutRedirectUris.add(postLogoutRedirectUri);
            return this;
        }

        public Builder postLogoutRedirectUris(Consumer<Set<String>> postLogoutRedirectUrisConsumer) {
            postLogoutRedirectUrisConsumer.accept(this.postLogoutRedirectUris);
            return this;
        }

        public Builder scope(String scope) {
            this.scopes.add(scope);
            return this;
        }

        public Builder scopes(Consumer<Set<String>> scopesConsumer) {
            scopesConsumer.accept(this.scopes);
            return this;
        }

        public Builder clientSettings(ClientSettings clientSettings) {
            this.clientSettings = clientSettings;
            return this;
        }

        public Builder tokenSettings(TokenSettings tokenSettings) {
            this.tokenSettings = tokenSettings;
            return this;
        }

        public RegisteredClient build() {
            Assert.hasText(this.clientId, "clientId cannot be empty");
            Assert.notEmpty(this.authorizationGrantTypes, "authorizationGrantTypes cannot be empty");
            if (this.authorizationGrantTypes.contains(AuthorizationGrantType.AUTHORIZATION_CODE)) {
                Assert.notEmpty(this.redirectUris, "redirectUris cannot be empty");
            }

            if (!StringUtils.hasText(this.clientName)) {
                this.clientName = this.id;
            }

            if (CollectionUtils.isEmpty(this.clientAuthenticationMethods)) {
                this.clientAuthenticationMethods.add(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
            }

            if (this.clientSettings == null) {
                ClientSettings.Builder builder = ClientSettings.builder();
                if (this.isPublicClientType()) {
                    builder.requireProofKey(true).requireAuthorizationConsent(true);
                }

                this.clientSettings = builder.build();
            }

            if (this.tokenSettings == null) {
                this.tokenSettings = TokenSettings.builder().build();
            }

            this.validateScopes();
            this.validateRedirectUris();
            this.validatePostLogoutRedirectUris();
            return this.create();
        }

        private boolean isPublicClientType() {
            return this.authorizationGrantTypes.contains(AuthorizationGrantType.AUTHORIZATION_CODE) && this.clientAuthenticationMethods.size() == 1 && this.clientAuthenticationMethods.contains(ClientAuthenticationMethod.NONE);
        }

        private RegisteredClient create() {
            RegisteredClient registeredClient = new RegisteredClient();
            registeredClient.id = this.id;
            registeredClient.clientId = this.clientId;
            registeredClient.clientIdIssuedAt = this.clientIdIssuedAt;
            registeredClient.clientSecret = this.clientSecret;
            registeredClient.clientSecretExpiresAt = this.clientSecretExpiresAt;
            registeredClient.clientName = this.clientName;
            registeredClient.clientAuthenticationMethods = Collections.unmodifiableSet(new HashSet(this.clientAuthenticationMethods));
            registeredClient.authorizationGrantTypes = Collections.unmodifiableSet(new HashSet(this.authorizationGrantTypes));
            registeredClient.redirectUris = Collections.unmodifiableSet(new HashSet(this.redirectUris));
            registeredClient.postLogoutRedirectUris = Collections.unmodifiableSet(new HashSet(this.postLogoutRedirectUris));
            registeredClient.scopes = Collections.unmodifiableSet(new HashSet(this.scopes));
            registeredClient.clientSettings = this.clientSettings;
            registeredClient.tokenSettings = this.tokenSettings;
            registeredClient.mediateToken = this.mediateToken;
            return registeredClient;
        }

        private void validateScopes() {
            if (!CollectionUtils.isEmpty(this.scopes)) {
                Iterator var1 = this.scopes.iterator();

                while(var1.hasNext()) {
                    String scope = (String)var1.next();
                    Assert.isTrue(validateScope(scope), "scope \"" + scope + "\" contains invalid characters");
                }

            }
        }

        private static boolean validateScope(String scope) {
            return scope == null || scope.chars().allMatch((c) -> {
                return withinTheRangeOf(c, 33, 33) || withinTheRangeOf(c, 35, 91) || withinTheRangeOf(c, 93, 126);
            });
        }

        private static boolean withinTheRangeOf(int c, int min, int max) {
            return c >= min && c <= max;
        }

        private void validateRedirectUris() {
            if (!CollectionUtils.isEmpty(this.redirectUris)) {
                Iterator var1 = this.redirectUris.iterator();

                while(var1.hasNext()) {
                    String redirectUri = (String)var1.next();
                    Assert.isTrue(validateRedirectUri(redirectUri), "redirect_uri \"" + redirectUri + "\" is not a valid redirect URI or contains fragment");
                }

            }
        }

        private void validatePostLogoutRedirectUris() {
            if (!CollectionUtils.isEmpty(this.postLogoutRedirectUris)) {
                Iterator var1 = this.postLogoutRedirectUris.iterator();

                while(var1.hasNext()) {
                    String postLogoutRedirectUri = (String)var1.next();
                    Assert.isTrue(validateRedirectUri(postLogoutRedirectUri), "post_logout_redirect_uri \"" + postLogoutRedirectUri + "\" is not a valid post logout redirect URI or contains fragment");
                }

            }
        }

        private static boolean validateRedirectUri(String redirectUri) {
            try {
                URI validRedirectUri = new URI(redirectUri);
                return validRedirectUri.getFragment() == null;
            } catch (URISyntaxException var2) {
                return false;
            }
        }

        static {
            serialVersionUID = SpringAuthorizationServerVersion.SERIAL_VERSION_UID;
        }
    }
}
