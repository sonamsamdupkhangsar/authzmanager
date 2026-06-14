package me.sonam.authzmanager.clients.user;

import java.util.Objects;
import java.util.UUID;

public
class OrganizationChoice {
    private Boolean selected = false;
    private Boolean defaultOrganization = false;
    private UUID organizationId;

    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }

    public Boolean getDefaultOrganization() {
        return defaultOrganization;
    }

    public void setDefaultOrganization(Boolean defaultOrganization) {
        this.defaultOrganization = defaultOrganization;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public OrganizationChoice() {
    }

    public OrganizationChoice(UUID organizationId) {
        this.organizationId = organizationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrganizationChoice that = (OrganizationChoice) o;
        return Objects.equals(selected, that.selected)
                && Objects.equals(defaultOrganization, that.defaultOrganization)
                && Objects.equals(organizationId, that.organizationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(selected, defaultOrganization, organizationId);
    }

    @Override
    public String toString() {
        return "OrganizationChoice{" +
                "selected=" + selected +
                ", defaultOrganization=" + defaultOrganization +
                ", organizationId=" + organizationId +
                '}';
    }
}
