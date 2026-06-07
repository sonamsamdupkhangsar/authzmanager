package me.sonam.authzmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "authzmanager.user-search")
public class UserSearchPolicyProperties {
    private final Map<String, HostPolicy> hosts = new LinkedHashMap<>();

    public Map<String, HostPolicy> getHosts() {
        return hosts;
    }

    public static class HostPolicy {
        private final List<String> allowedEmailDomains = new ArrayList<>();

        public List<String> getAllowedEmailDomains() {
            return allowedEmailDomains;
        }
    }
}
