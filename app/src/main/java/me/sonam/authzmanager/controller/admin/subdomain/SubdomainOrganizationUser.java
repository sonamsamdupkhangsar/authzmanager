package me.sonam.authzmanager.controller.admin.subdomain;

import java.util.UUID;

public record SubdomainOrganizationUser(UUID userId, UUID organizationId, String organizationName) {
}
