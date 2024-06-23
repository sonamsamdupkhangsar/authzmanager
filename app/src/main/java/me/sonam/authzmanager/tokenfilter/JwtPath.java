package me.sonam.authzmanager.tokenfilter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a property object for `jwtrequest`
 */
@Component
@ConfigurationProperties
public class JwtPath {
    private List<JwtRequest> jwtrequest = new ArrayList();

    public List<JwtRequest> getJwtRequest() {
        return jwtrequest;
    }

    private Map<String, List> map = new HashMap<>();

    public JwtPath() {

    }

    public static class JwtRequest {
        private String out;
        private AccessToken accessToken;

        public JwtRequest() {
        }

        public String getOut() {
            return out;
        }

        public void setOut(String out) {
            this.out = out;
        }

        public AccessToken getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(AccessToken accessToken) {
            this.accessToken = accessToken;
        }

        @Override
        public String toString() {
            return "JwtRequest{" +
                    ", out='" + out + '\'' +
                    ", accessToken='" + accessToken + '\'' +
                    '}';
        }

        public static class AccessToken {
            public static enum JwtOption {
                forward, request, doNothing
            }

            private JwtOption option;
            private String scopes;
            private String base64EncodedClientIdSecret;

            public AccessToken(String option, String scopes, String base64EncodedClientIdSecret) {
                this.option = JwtOption.valueOf(option);
                this.scopes = scopes;
                this.base64EncodedClientIdSecret = base64EncodedClientIdSecret;
            }

            public JwtOption getOption() {
                return option;
            }
            public String getScopes() {
                return scopes;
            }
            public String getBase64EncodedClientIdSecret() {
                return base64EncodedClientIdSecret;
            }

            @Override
            public String toString() {
                return "AccessToken{" +
                        "option=" + option +
                        ", scopes='" + scopes + '\'' +
                        ", base64EncodedClientIdSecret='" + base64EncodedClientIdSecret + '\'' +
                        '}';
            }
        }
    }
}
