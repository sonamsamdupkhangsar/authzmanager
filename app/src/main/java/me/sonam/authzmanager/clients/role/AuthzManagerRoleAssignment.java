package me.sonam.authzmanager.clients.role;

import java.util.UUID;

/**
 * Client representation of the role-rest-service AuthzManagerRoleAssignment entity.
 */
public record AuthzManagerRoleAssignment(UUID id, UUID authzManagerRoleId, UUID userId,
                                         String scopeType, UUID scopeId) {
}
