package me.sonam.authzmanager.config;

import me.sonam.authzmanager.tenant.TenantAuthorizationUrlResolver;
import me.sonam.authzmanager.tenant.TenantAwareClientRegistrationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.filter.ForwardedHeaderFilter;

/**
 * Replaces static OAuth provider configuration with request-host-aware client registration resolution.
 */
@Configuration
public class TenantAwareOAuth2ClientConfig {
    /**
     * Honors forwarded headers so host-based issuer resolution works behind proxies and ingress.
     */
    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }

    /**
     * Builds the client registration repository that derives provider endpoints from the current request host.
     */
    @Bean
    @Primary
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${authzmanager.oauth2.registration-id}") String registrationId,
            @Value("${authzmanager.oauth2.client-id}") String clientId,
            @Value("${authzmanager.oauth2.client-secret}") String clientSecret,
            TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver) {
        return new TenantAwareClientRegistrationRepository(
                registrationId, clientId, clientSecret, tenantAuthorizationUrlResolver);
    }
}
