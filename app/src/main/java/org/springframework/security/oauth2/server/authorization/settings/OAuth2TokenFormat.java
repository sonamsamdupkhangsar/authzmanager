package org.springframework.security.oauth2.server.authorization.settings;

import me.sonam.authzmanager.controller.admin.oauth2.SpringAuthorizationServerVersion;
import org.springframework.util.Assert;

import java.io.Serializable;

public final class OAuth2TokenFormat implements Serializable {
    private static final long serialVersionUID;
    public static final OAuth2TokenFormat SELF_CONTAINED;
    public static final OAuth2TokenFormat REFERENCE;
    private final String value;

    public OAuth2TokenFormat(String value) {
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
            OAuth2TokenFormat that = (OAuth2TokenFormat)obj;
            return this.getValue().equals(that.getValue());
        } else {
            return false;
        }
    }

    public int hashCode() {
        return this.getValue().hashCode();
    }

    static {
        serialVersionUID = SpringAuthorizationServerVersion.SERIAL_VERSION_UID;
        SELF_CONTAINED = new OAuth2TokenFormat("self-contained");
        REFERENCE = new OAuth2TokenFormat("reference");
    }


}
