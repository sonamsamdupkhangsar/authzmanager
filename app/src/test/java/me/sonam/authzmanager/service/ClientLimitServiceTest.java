package me.sonam.authzmanager.service;

import me.sonam.authzmanager.config.ClientLimitProperties;
import me.sonam.authzmanager.tenant.TenantAuthorizationUrlResolver;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientLimitServiceTest {

    @Test
    void resolvesClientLimitForCurrentTenantAuthorizationHost() {
        ClientLimitProperties properties = new ClientLimitProperties();
        properties.setDefaultMaxClients(5);
        properties.getHosts().put("free.openissuer.test", 2);

        TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver = mock(TenantAuthorizationUrlResolver.class);
        when(tenantAuthorizationUrlResolver.currentAuthorizationHost()).thenReturn("free.openissuer.test");

        ClientLimitService service = new ClientLimitService(properties, tenantAuthorizationUrlResolver);

        assertThat(service.maxClientsForCurrentTenant()).isEqualTo(2);
    }

    @Test
    void usesDefaultClientLimitWhenCurrentTenantHasNoOverride() {
        ClientLimitProperties properties = new ClientLimitProperties();
        properties.setDefaultMaxClients(5);
        properties.getHosts().put("free.openissuer.test", 2);

        TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver = mock(TenantAuthorizationUrlResolver.class);
        when(tenantAuthorizationUrlResolver.currentAuthorizationHost()).thenReturn("business1.openissuer.test");

        ClientLimitService service = new ClientLimitService(properties, tenantAuthorizationUrlResolver);

        assertThat(service.maxClientsForCurrentTenant()).isEqualTo(5);
    }
}
