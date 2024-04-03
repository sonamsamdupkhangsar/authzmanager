package me.sonam.authzmanager.controller.admin.oauth2;


import org.springframework.util.Assert;

import java.io.Serializable;

public final class ClientAuthenticationMethod implements Serializable {
    private static final long serialVersionUID = 610L;
    public static final ClientAuthenticationMethod CLIENT_SECRET_BASIC = new ClientAuthenticationMethod("client_secret_basic");
    public static final ClientAuthenticationMethod CLIENT_SECRET_POST = new ClientAuthenticationMethod("client_secret_post");
    public static final ClientAuthenticationMethod CLIENT_SECRET_JWT = new ClientAuthenticationMethod("client_secret_jwt");
    public static final ClientAuthenticationMethod PRIVATE_KEY_JWT = new ClientAuthenticationMethod("private_key_jwt");
    public static final ClientAuthenticationMethod NONE = new ClientAuthenticationMethod("none");
    private final String value;

    public ClientAuthenticationMethod(String value) {
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
            ClientAuthenticationMethod that = (ClientAuthenticationMethod)obj;
            return this.getValue().equals(that.getValue());
        } else {
            return false;
        }
    }

    public int hashCode() {
        return this.getValue().hashCode();
    }


}