package me.sonam.authzmanager;

import me.sonam.authzmanager.controller.admin.oauth2.ClientAuthenticationMethod;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientAuthenticationMethodTest {
    private static final Logger LOG = LoggerFactory.getLogger(ClientAuthenticationMethodTest.class);

    @Test
    public void printTest() {
        String s = "CLIENT_SECRET_BASIC";

        LOG.info("ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue(): {}", ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue());
        if (ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue().equals(s)) {
            LOG.info("clientAuthenitcation: {}, value: {}", ClientAuthenticationMethod.CLIENT_SECRET_BASIC, ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue());
        }
        else {
            LOG.error("not equals");
        }
    }
}
