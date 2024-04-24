package me.sonam.authzmanager.clients.user;

import me.sonam.authzmanager.controller.admin.organization.Organization;

import java.util.UUID;

public class ClientOrganization {
    private UUID clientId;
    private UUID organizationId;
    private boolean selected;
    private Organization organization;

    public ClientOrganization() {

    }
    public ClientOrganization(UUID clientId, UUID organizationId) {
        this.clientId = clientId;
        this.organizationId = organizationId;
    }

    public ClientOrganization(UUID clientId, Organization organization, boolean selected) {
        this.clientId = clientId;
        this.organization = organization;
        this.selected = selected;
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

    public boolean getSelected() {
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
        return "ClientOrganization{" +
                "clientId=" + clientId +
                ", organizationId=" + organizationId +
                ", selected=" + selected +
                ", organization=" + organization +
                '}';
    }
}
