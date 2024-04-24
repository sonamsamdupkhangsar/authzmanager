package me.sonam.authzmanager.controller.admin.oauth2;

import java.time.Duration;
import java.util.Map;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.util.Assert;
import me.sonam.authzmanager.controller.admin.oauth2.ConfigurationSettingNames.Token;
public final class TokenSettings extends AbstractSettings {
    private TokenSettings(Map<String, Object> settings) {
        super(settings);
    }

    public Duration getAuthorizationCodeTimeToLive() {
        return (Duration)this.getSetting(Token.AUTHORIZATION_CODE_TIME_TO_LIVE);
    }

    public Duration getAccessTokenTimeToLive() {
        return (Duration)this.getSetting(Token.ACCESS_TOKEN_TIME_TO_LIVE);
    }

    public OAuth2TokenFormat getAccessTokenFormat() {
        return (OAuth2TokenFormat)this.getSetting(Token.ACCESS_TOKEN_FORMAT);
    }

    public Duration getDeviceCodeTimeToLive() {
        return (Duration)this.getSetting(Token.DEVICE_CODE_TIME_TO_LIVE);
    }

    public boolean isReuseRefreshTokens() {
        return (Boolean)this.getSetting(Token.REUSE_REFRESH_TOKENS);
    }

    public Duration getRefreshTokenTimeToLive() {
        return (Duration)this.getSetting(Token.REFRESH_TOKEN_TIME_TO_LIVE);
    }

    public SignatureAlgorithm getIdTokenSignatureAlgorithm() {
        return (SignatureAlgorithm)this.getSetting(Token.ID_TOKEN_SIGNATURE_ALGORITHM);
    }

    public static Builder builder() {
        return (new Builder()).authorizationCodeTimeToLive(Duration.ofMinutes(5L)).accessTokenTimeToLive(Duration.ofMinutes(5L)).accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED).deviceCodeTimeToLive(Duration.ofMinutes(5L)).reuseRefreshTokens(true).refreshTokenTimeToLive(Duration.ofMinutes(60L)).idTokenSignatureAlgorithm(SignatureAlgorithm.RS256);
    }

    public static Builder withSettings(Map<String, Object> settings) {
        Assert.notEmpty(settings, "settings cannot be empty");
        return (Builder)(new Builder()).settings((s) -> {
            s.putAll(settings);
        });
    }

    public static final class Builder extends AbstractSettings.AbstractBuilder<TokenSettings, Builder> {
        private Builder() {
        }

        public Builder authorizationCodeTimeToLive(Duration authorizationCodeTimeToLive) {
            Assert.notNull(authorizationCodeTimeToLive, "authorizationCodeTimeToLive cannot be null");
            Assert.isTrue(authorizationCodeTimeToLive.getSeconds() > 0L, "authorizationCodeTimeToLive must be greater than Duration.ZERO");
            return (Builder)this.setting(Token.AUTHORIZATION_CODE_TIME_TO_LIVE, authorizationCodeTimeToLive);
        }

        public Builder accessTokenTimeToLive(Duration accessTokenTimeToLive) {
            Assert.notNull(accessTokenTimeToLive, "accessTokenTimeToLive cannot be null");
            Assert.isTrue(accessTokenTimeToLive.getSeconds() > 0L, "accessTokenTimeToLive must be greater than Duration.ZERO");
            return (Builder)this.setting(Token.ACCESS_TOKEN_TIME_TO_LIVE, accessTokenTimeToLive);
        }

        public Builder accessTokenFormat(OAuth2TokenFormat accessTokenFormat) {
            Assert.notNull(accessTokenFormat, "accessTokenFormat cannot be null");
            return (Builder)this.setting(Token.ACCESS_TOKEN_FORMAT, accessTokenFormat);
        }

        public Builder deviceCodeTimeToLive(Duration deviceCodeTimeToLive) {
            Assert.notNull(deviceCodeTimeToLive, "deviceCodeTimeToLive cannot be null");
            Assert.isTrue(deviceCodeTimeToLive.getSeconds() > 0L, "deviceCodeTimeToLive must be greater than Duration.ZERO");
            return (Builder)this.setting(Token.DEVICE_CODE_TIME_TO_LIVE, deviceCodeTimeToLive);
        }

        public Builder reuseRefreshTokens(boolean reuseRefreshTokens) {
            return (Builder)this.setting(Token.REUSE_REFRESH_TOKENS, reuseRefreshTokens);
        }

        public Builder refreshTokenTimeToLive(Duration refreshTokenTimeToLive) {
            Assert.notNull(refreshTokenTimeToLive, "refreshTokenTimeToLive cannot be null");
            Assert.isTrue(refreshTokenTimeToLive.getSeconds() > 0L, "refreshTokenTimeToLive must be greater than Duration.ZERO");
            return (Builder)this.setting(Token.REFRESH_TOKEN_TIME_TO_LIVE, refreshTokenTimeToLive);
        }

        public Builder idTokenSignatureAlgorithm(SignatureAlgorithm idTokenSignatureAlgorithm) {
            Assert.notNull(idTokenSignatureAlgorithm, "idTokenSignatureAlgorithm cannot be null");
            return (Builder)this.setting(Token.ID_TOKEN_SIGNATURE_ALGORITHM, idTokenSignatureAlgorithm);
        }

        public TokenSettings build() {
            return new TokenSettings(this.getSettings());
        }
    }
}
