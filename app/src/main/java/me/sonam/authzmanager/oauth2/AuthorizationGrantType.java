package me.sonam.authzmanager.oauth2;

import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;

public final class AuthorizationGrantType implements Serializable {
    @Serial
    private static final long serialVersionUID = 610L;
    public static final AuthorizationGrantType AUTHORIZATION_CODE = new AuthorizationGrantType("authorization_code");
    public static final AuthorizationGrantType CLIENT_CREDENTIALS = new AuthorizationGrantType("client_credentials");
    public static final AuthorizationGrantType REFRESH_TOKEN = new AuthorizationGrantType("refresh_token");
    public static final AuthorizationGrantType DEVICE_CODE = new AuthorizationGrantType("urn:ietf:params:oauth:grant-type:device_code");
    public static final AuthorizationGrantType TOKEN_EXCHANGE = new AuthorizationGrantType("urn:ietf:params:oauth:grant-type:token-exchange");
    public static final AuthorizationGrantType JWT_BEARER = new AuthorizationGrantType("urn:ietf:params:oauth:grant-type:jwt-bearer");

    private final String value;

    public AuthorizationGrantType(String value) {
        Assert.hasText(value, "value cannot be empty");
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj != null && this.getClass() == obj.getClass()) {
            AuthorizationGrantType that = (AuthorizationGrantType)obj;
            return this.getValue().equals(that.getValue());
        } else {
            return false;
        }
    }

    public int hashCode() {
        return this.getValue().hashCode();
    }

    @Override
    public String toString() {
        return "AuthorizationGrantType{" +
                "value='" + value + '\'' +
                '}';
    }
}