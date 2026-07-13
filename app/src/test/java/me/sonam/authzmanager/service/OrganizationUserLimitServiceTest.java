package me.sonam.authzmanager.service;

import me.sonam.authzmanager.config.OrganizationUserLimitProperties;
import me.sonam.authzmanager.tenant.TenantAuthorizationUrlResolver;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrganizationUserLimitServiceTest {

    @Test
    void resolvesLimitForCurrentTenantAuthorizationHost() {
        OrganizationUserLimitProperties properties = new OrganizationUserLimitProperties();
        properties.setDefaultMaxAddedUsers(5);
        properties.getHosts().put("free.openissuer.test", 2);

        TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver = mock(TenantAuthorizationUrlResolver.class);
        when(tenantAuthorizationUrlResolver.currentAuthorizationHost()).thenReturn("free.openissuer.test");

        OrganizationUserLimitService service = new OrganizationUserLimitService(
                properties, tenantAuthorizationUrlResolver);

        assertThat(service.maxAddedUsersForCurrentTenant()).isEqualTo(2);
    }

    @Test
    void usesDefaultLimitWhenCurrentTenantHasNoOverride() {
        OrganizationUserLimitProperties properties = new OrganizationUserLimitProperties();
        properties.setDefaultMaxAddedUsers(5);
        properties.getHosts().put("free.openissuer.test", 2);

        TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver = mock(TenantAuthorizationUrlResolver.class);
        when(tenantAuthorizationUrlResolver.currentAuthorizationHost()).thenReturn("business1.openissuer.test");

        OrganizationUserLimitService service = new OrganizationUserLimitService(
                properties, tenantAuthorizationUrlResolver);

        assertThat(service.maxAddedUsersForCurrentTenant()).isEqualTo(5);
    }
}
