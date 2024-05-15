package me.sonam.authzmanager.controller.admin.roles;

import me.sonam.authzmanager.controller.admin.organization.Organization;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;

import java.util.Objects;
import java.util.UUID;

public class RoleOrganization {

    private UUID id;

    private UUID roleId;
    private UUID organizationId;
    private boolean selected;
    private Organization organization;
    public RoleOrganization() {
    }

    public RoleOrganization(UUID id, UUID roleId, UUID organizationId, boolean selected) {
        this.id = id;
        this.roleId = roleId;
        this.organizationId = organizationId;
        this.selected = selected;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    @Override
    public String toString() {
        return "RoleOrganization{" +
                "id=" + id +
                ", roleId=" + roleId +
                ", organizationId=" + organizationId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoleOrganization that = (RoleOrganization) o;
        return Objects.equals(id, that.id) && Objects.equals(roleId, that.roleId) && Objects.equals(organizationId, that.organizationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, roleId, organizationId);
    }
}
