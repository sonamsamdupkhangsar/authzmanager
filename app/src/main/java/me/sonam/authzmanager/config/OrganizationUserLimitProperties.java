package me.sonam.authzmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "organization-user-limits")
public class OrganizationUserLimitProperties {
    private int defaultMaxAddedUsers = 5;
    private final Map<String, Integer> hosts = new LinkedHashMap<>();

    public int maxAddedUsersForHost(String host) {
        if (host == null || host.isBlank()) {
            return defaultMaxAddedUsers;
        }
        return hosts.getOrDefault(host, defaultMaxAddedUsers);
    }

    public int getDefaultMaxAddedUsers() {
        return defaultMaxAddedUsers;
    }

    public void setDefaultMaxAddedUsers(int defaultMaxAddedUsers) {
        this.defaultMaxAddedUsers = defaultMaxAddedUsers;
    }

    public Map<String, Integer> getHosts() {
        return hosts;
    }
}
