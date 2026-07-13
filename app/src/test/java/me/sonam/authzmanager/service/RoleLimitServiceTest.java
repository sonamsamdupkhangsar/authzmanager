package me.sonam.authzmanager.service;

import me.sonam.authzmanager.config.RoleLimitProperties;
import me.sonam.authzmanager.tenant.TenantAuthorizationUrlResolver;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoleLimitServiceTest {

    @Test
    void resolvesRoleLimitForCurrentTenantAuthorizationHost() {
        RoleLimitProperties properties = new RoleLimitProperties();
        properties.setDefaultMaxRoles(5);
        properties.getHosts().put("free.openissuer.test", 2);

        TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver = mock(TenantAuthorizationUrlResolver.class);
        when(tenantAuthorizationUrlResolver.currentAuthorizationHost()).thenReturn("free.openissuer.test");

        RoleLimitService service = new RoleLimitService(properties, tenantAuthorizationUrlResolver);

        assertThat(service.maxRolesForCurrentTenant()).isEqualTo(2);
    }

    @Test
    void usesDefaultRoleLimitWhenCurrentTenantHasNoOverride() {
        RoleLimitProperties properties = new RoleLimitProperties();
        properties.setDefaultMaxRoles(5);
        properties.getHosts().put("free.openissuer.test", 2);

        TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver = mock(TenantAuthorizationUrlResolver.class);
        when(tenantAuthorizationUrlResolver.currentAuthorizationHost()).thenReturn("business1.openissuer.test");

        RoleLimitService service = new RoleLimitService(properties, tenantAuthorizationUrlResolver);

        assertThat(service.maxRolesForCurrentTenant()).isEqualTo(5);
    }
}
