package me.sonam.authzmanager.service;

import me.sonam.authzmanager.config.UserSearchPolicyProperties;
import me.sonam.authzmanager.tenant.TenantAuthorizationUrlResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class UserSearchPolicyService {
    private final UserSearchPolicyProperties userSearchPolicyProperties;
    private final TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver;

    public UserSearchPolicyService(UserSearchPolicyProperties userSearchPolicyProperties,
                                   TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver) {
        this.userSearchPolicyProperties = userSearchPolicyProperties;
        this.tenantAuthorizationUrlResolver = tenantAuthorizationUrlResolver;
    }

    public Optional<String> validateSearch(String authenticationId) {
        String currentHost = tenantAuthorizationUrlResolver.currentAuthorizationHost();
        UserSearchPolicyProperties.HostPolicy policy = userSearchPolicyProperties.getHosts().get(currentHost);
        if (policy == null || policy.getAllowedEmailDomains().isEmpty()) {
            return Optional.empty();
        }

        String normalizedAuthenticationId = normalize(authenticationId);
        int at = normalizedAuthenticationId.lastIndexOf('@');
        if (at < 0 || at == normalizedAuthenticationId.length() - 1) {
            return Optional.empty();
        }

        String emailDomain = normalizedAuthenticationId.substring(at + 1);
        List<String> allowedDomains = policy.getAllowedEmailDomains();
        boolean allowsWildcard = allowedDomains.stream().map(this::normalize).anyMatch("*"::equals);
        if (allowsWildcard) {
            return isDomainReservedForAnotherHost(currentHost, emailDomain)
                    ? Optional.of("user belongs to another subdomain")
                    : Optional.empty();
        }

        boolean allowed = allowedDomains.stream()
                .map(this::normalize)
                .anyMatch(domain -> domain.equals(emailDomain));

        return allowed ? Optional.empty() : Optional.of("user belongs to another subdomain");
    }

    private boolean isDomainReservedForAnotherHost(String host, String emailDomain) {
        String normalizedHost = normalize(host);
        return userSearchPolicyProperties.getHosts().entrySet().stream()
                .filter(entry -> !normalize(entry.getKey()).equals(normalizedHost))
                .flatMap(entry -> entry.getValue().getAllowedEmailDomains().stream())
                .map(this::normalize)
                .filter(domain -> !domain.equals("*"))
                .anyMatch(domain -> domain.equals(emailDomain));
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
    }
}
