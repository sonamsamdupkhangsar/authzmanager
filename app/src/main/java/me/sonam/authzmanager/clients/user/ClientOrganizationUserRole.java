package me.sonam.authzmanager.clients.user;

import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;

import java.util.Objects;
import java.util.UUID;

public class ClientOrganizationUserRole {
    private UUID id;

    private UUID roleId;
    private UUID clientId;
    private UUID organizationId;
    private UUID userId;

    private boolean isNew;

    public UUID getId() {
        return id;
    }

    public boolean isNew() {
        return isNew;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientOrganizationUserRole that = (ClientOrganizationUserRole) o;
        return isNew == that.isNew && Objects.equals(id, that.id) && Objects.equals(roleId, that.roleId) && Objects.equals(clientId, that.clientId) && Objects.equals(organizationId, that.organizationId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, roleId, clientId, organizationId, userId, isNew);
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    @Override
    public String toString() {
        return "RoleClientOrganizationUser{" +
                "id=" + id +
                ", roleId=" + roleId +
                ", clientId=" + clientId +
                ", organizationId=" + organizationId +
                ", userId=" + userId +
                ", isNew=" + isNew +
                '}';
    }
}

