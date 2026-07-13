package me.sonam.authzmanager.service;

import me.sonam.authzmanager.config.OrganizationUserLimitProperties;
import me.sonam.authzmanager.tenant.TenantAuthorizationUrlResolver;
import org.springframework.stereotype.Service;

@Service
public class OrganizationUserLimitService {
    private final OrganizationUserLimitProperties organizationUserLimitProperties;
    private final TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver;

    public OrganizationUserLimitService(OrganizationUserLimitProperties organizationUserLimitProperties,
                                        TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver) {
        this.organizationUserLimitProperties = organizationUserLimitProperties;
        this.tenantAuthorizationUrlResolver = tenantAuthorizationUrlResolver;
    }

    public int maxAddedUsersForCurrentTenant() {
        return organizationUserLimitProperties.maxAddedUsersForHost(
                tenantAuthorizationUrlResolver.currentAuthorizationHost());
    }

    public int maxAddedUsersForHost(String host) {
        return organizationUserLimitProperties.maxAddedUsersForHost(host);
    }
}
