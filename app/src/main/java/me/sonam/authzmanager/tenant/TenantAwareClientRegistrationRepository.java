package me.sonam.authzmanager.tenant;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.Iterator;
import java.util.List;

/**
 * Creates a single OAuth client registration whose provider endpoints are derived per request host.
 */
public class TenantAwareClientRegistrationRepository
        implements ClientRegistrationRepository, Iterable<ClientRegistration> {
    private final String registrationId;
    private final String clientId;
    private final String clientSecret;
    private final TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver;

    public TenantAwareClientRegistrationRepository(String registrationId, String clientId, String clientSecret,
                                                   TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver) {
        this.registrationId = registrationId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tenantAuthorizationUrlResolver = tenantAuthorizationUrlResolver;
    }

    /**
     * Returns the tenant-specific registration when the configured registration id is requested.
     */
    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        if (!this.registrationId.equals(registrationId)) {
            return null;
        }
        return clientRegistration();
    }

    /**
     * Exposes the single tenant-specific registration for iteration-based Spring Security lookups.
     */
    @Override
    public Iterator<ClientRegistration> iterator() {
        return List.of(clientRegistration()).iterator();
    }

    /**
     * Builds the tenant-specific client registration using the issuer resolved from the current request.
     */
    private ClientRegistration clientRegistration() {
        String issuerUri = tenantAuthorizationUrlResolver.currentIssuerUri();

        return ClientRegistration.withRegistrationId(registrationId)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile")
                .authorizationUri(issuerUri + "/oauth2/authorize")
                .tokenUri(issuerUri + "/oauth2/token")
                .userInfoUri(issuerUri + "/userinfo")
                .userNameAttributeName("sub")
                .jwkSetUri(issuerUri + "/oauth2/jwks")
                .issuerUri(issuerUri)
                .clientName(registrationId)
                .build();
    }
}
