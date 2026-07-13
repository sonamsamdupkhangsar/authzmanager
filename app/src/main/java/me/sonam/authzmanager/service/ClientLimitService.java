package me.sonam.authzmanager.service;

import me.sonam.authzmanager.config.ClientLimitProperties;
import me.sonam.authzmanager.tenant.TenantAuthorizationUrlResolver;
import org.springframework.stereotype.Service;

@Service
public class ClientLimitService {
    private final ClientLimitProperties clientLimitProperties;
    private final TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver;

    public ClientLimitService(ClientLimitProperties clientLimitProperties,
                              TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver) {
        this.clientLimitProperties = clientLimitProperties;
        this.tenantAuthorizationUrlResolver = tenantAuthorizationUrlResolver;
    }

    public int maxClientsForCurrentTenant() {
        return clientLimitProperties.maxClientsForHost(tenantAuthorizationUrlResolver.currentAuthorizationHost());
    }

    public int maxClientsForHost(String host) {
        return clientLimitProperties.maxClientsForHost(host);
    }
}
