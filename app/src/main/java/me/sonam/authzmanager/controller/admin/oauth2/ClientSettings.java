package me.sonam.authzmanager.controller.admin.oauth2;

import java.util.Map;
import org.springframework.util.Assert;
import me.sonam.authzmanager.controller.admin.oauth2.ConfigurationSettingNames.Client;

public final class ClientSettings extends AbstractSettings {
    private ClientSettings(Map<String, Object> settings) {
        super(settings);
    }

    public boolean isRequireProofKey() {
        return (Boolean)this.getSetting(Client.REQUIRE_PROOF_KEY);
    }

    public boolean isRequireAuthorizationConsent() {
        return (Boolean)this.getSetting(Client.REQUIRE_AUTHORIZATION_CONSENT);
    }

    public String getJwkSetUrl() {
        return (String)this.getSetting(Client.JWK_SET_URL);
    }

    public JwsAlgorithm getTokenEndpointAuthenticationSigningAlgorithm() {
        return (JwsAlgorithm)this.getSetting(Client.TOKEN_ENDPOINT_AUTHENTICATION_SIGNING_ALGORITHM);
    }

    public static Builder builder() {
        return (new Builder()).requireProofKey(false).requireAuthorizationConsent(false);
    }

    public static Builder withSettings(Map<String, Object> settings) {
        Assert.notEmpty(settings, "settings cannot be empty");
        return (Builder)(new Builder()).settings((s) -> {
            s.putAll(settings);
        });
    }

    public static final class Builder extends AbstractSettings.AbstractBuilder<ClientSettings, Builder> {
        private Builder() {
        }

        public Builder requireProofKey(boolean requireProofKey) {
            return (Builder)this.setting(Client.REQUIRE_PROOF_KEY, requireProofKey);
        }

        public Builder requireAuthorizationConsent(boolean requireAuthorizationConsent) {
            return (Builder)this.setting(Client.REQUIRE_AUTHORIZATION_CONSENT, requireAuthorizationConsent);
        }

        public Builder jwkSetUrl(String jwkSetUrl) {
            return (Builder)this.setting(Client.JWK_SET_URL, jwkSetUrl);
        }

        public Builder tokenEndpointAuthenticationSigningAlgorithm(JwsAlgorithm authenticationSigningAlgorithm) {
            return (Builder)this.setting(Client.TOKEN_ENDPOINT_AUTHENTICATION_SIGNING_ALGORITHM, authenticationSigningAlgorithm);
        }

        public ClientSettings build() {
            return new ClientSettings(this.getSettings());
        }
    }


}
