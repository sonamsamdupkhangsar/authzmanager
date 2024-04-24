package org.springframework.security.oauth2.jose.jws;

import me.sonam.authzmanager.controller.admin.oauth2.JwsAlgorithm;

public enum SignatureAlgorithm implements JwsAlgorithm {
    RS256("RS256"),
    RS384("RS384"),
    RS512("RS512"),
    ES256("ES256"),
    ES384("ES384"),
    ES512("ES512"),
    PS256("PS256"),
    PS384("PS384"),
    PS512("PS512");

    private final String name;

    private SignatureAlgorithm(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public static SignatureAlgorithm from(String name) {
        SignatureAlgorithm[] var1 = values();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            SignatureAlgorithm value = var1[var3];
            if (value.getName().equals(name)) {
                return value;
            }
        }

        return null;
    }
}
