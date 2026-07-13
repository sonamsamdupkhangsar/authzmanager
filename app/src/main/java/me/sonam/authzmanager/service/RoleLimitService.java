package me.sonam.authzmanager.service;

import me.sonam.authzmanager.config.RoleLimitProperties;
import me.sonam.authzmanager.tenant.TenantAuthorizationUrlResolver;
import org.springframework.stereotype.Service;

@Service
public class RoleLimitService {
    private final RoleLimitProperties roleLimitProperties;
    private final TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver;

    public RoleLimitService(RoleLimitProperties roleLimitProperties,
                            TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver) {
        this.roleLimitProperties = roleLimitProperties;
        this.tenantAuthorizationUrlResolver = tenantAuthorizationUrlResolver;
    }

    public int maxRolesForCurrentTenant() {
        return roleLimitProperties.maxRolesForHost(tenantAuthorizationUrlResolver.currentAuthorizationHost());
    }

    public int maxRolesForHost(String host) {
        return roleLimitProperties.maxRolesForHost(host);
    }
}
