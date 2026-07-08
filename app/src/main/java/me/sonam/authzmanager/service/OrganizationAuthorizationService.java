package me.sonam.authzmanager.service;

import me.sonam.authzmanager.controller.admin.organization.Organization;
import me.sonam.authzmanager.webclients.OrganizationWebClient;
import me.sonam.authzmanager.webclients.RoleWebClient;
import org.apache.tomcat.websocket.AuthenticationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class OrganizationAuthorizationService {
    private final OrganizationWebClient organizationWebClient;
    private final RoleWebClient roleWebClient;

    public OrganizationAuthorizationService(OrganizationWebClient organizationWebClient,
                                            RoleWebClient roleWebClient) {
        this.organizationWebClient = organizationWebClient;
        this.roleWebClient = roleWebClient;
    }

    public Mono<Organization> requireOrgAdminOrSubdomainAdmin(String accessToken, UUID userId,
                                                              String organizationHost,
                                                              Organization organization) {
        return roleWebClient.isOrgAdminInOrgId(accessToken, userId, organization.getId())
                .flatMap(isOrgAdmin -> {
                    if (isOrgAdmin) {
                        return Mono.just(organization);
                    }
                    return requireSubdomainAdminForOrganization(accessToken, userId, organizationHost, organization);
                });
    }

    public Mono<Organization> requireSubdomainAdminForOrganization(String accessToken, UUID userId,
                                                                   String organizationHost,
                                                                   Organization organization) {
        return organizationWebClient.getSubdomainByHost(accessToken, organizationHost)
                .flatMap(subdomain -> roleWebClient.isSubdomainAdminInSubdomainId(accessToken, userId,
                        subdomain.getId()))
                .flatMap(isSubdomainAdmin -> {
                    if (!isSubdomainAdmin) {
                        return Mono.error(new AuthenticationException(
                                "You are not an OrgAdmin for orgId or SubdomainAdmin for subdomain: "
                                        + organization.getName()));
                    }
                    return organizationWebClient.organizationBelongsToSubdomain(accessToken,
                                    organization.getId(), organizationHost)
                            .thenReturn(organization);
                });
    }
}
