package me.sonam.authzmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "client-limits")
public class ClientLimitProperties {
    private int defaultMaxClients = 5;
    private final Map<String, Integer> hosts = new LinkedHashMap<>();

    public int maxClientsForHost(String host) {
        if (host == null || host.isBlank()) {
            return defaultMaxClients;
        }
        return hosts.getOrDefault(host, defaultMaxClients);
    }

    public int getDefaultMaxClients() {
        return defaultMaxClients;
    }

    public void setDefaultMaxClients(int defaultMaxClients) {
        this.defaultMaxClients = defaultMaxClients;
    }

    public Map<String, Integer> getHosts() {
        return hosts;
    }
}
