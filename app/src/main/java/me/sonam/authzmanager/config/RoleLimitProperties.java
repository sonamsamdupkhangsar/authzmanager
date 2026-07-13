package me.sonam.authzmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "role-limits")
public class RoleLimitProperties {
    private int defaultMaxRoles = 5;
    private final Map<String, Integer> hosts = new LinkedHashMap<>();

    public int maxRolesForHost(String host) {
        if (host == null || host.isBlank()) {
            return defaultMaxRoles;
        }
        return hosts.getOrDefault(host, defaultMaxRoles);
    }

    public int getDefaultMaxRoles() {
        return defaultMaxRoles;
    }

    public void setDefaultMaxRoles(int defaultMaxRoles) {
        this.defaultMaxRoles = defaultMaxRoles;
    }

    public Map<String, Integer> getHosts() {
        return hosts;
    }
}
