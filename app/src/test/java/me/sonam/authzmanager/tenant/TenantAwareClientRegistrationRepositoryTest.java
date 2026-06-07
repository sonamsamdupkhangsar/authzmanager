package me.sonam.authzmanager.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that OAuth client registrations point at the issuer derived from the current request host.
 */
class TenantAwareClientRegistrationRepositoryTest {
    /**
     * Clears request state between tests so client registration generation stays deterministic.
     */
    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * Verifies that OAuth provider endpoints are built for the tenant-specific authorization host.
     */
    @Test
    void buildsTenantSpecificProviderEndpoints() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("business1.admin.openissuer.com");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        TenantAuthorizationUrlResolver resolver =
                new TenantAuthorizationUrlResolver("admin", "https://platform.openissuer.com");
        TenantAwareClientRegistrationRepository repository =
                new TenantAwareClientRegistrationRepository("authzmanager", "client-id", "secret", resolver);

        ClientRegistration clientRegistration = repository.findByRegistrationId("authzmanager");

        assertEquals("https://business1.openissuer.com/oauth2/authorize", clientRegistration.getProviderDetails().getAuthorizationUri());
        assertEquals("https://business1.openissuer.com/oauth2/token", clientRegistration.getProviderDetails().getTokenUri());
        assertEquals("https://business1.openissuer.com/oauth2/jwks", clientRegistration.getProviderDetails().getJwkSetUri());
        assertEquals("https://business1.openissuer.com/userinfo", clientRegistration.getProviderDetails().getUserInfoEndpoint().getUri());
    }
}
