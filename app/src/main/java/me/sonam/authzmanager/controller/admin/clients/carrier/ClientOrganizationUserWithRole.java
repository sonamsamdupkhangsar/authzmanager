package me.sonam.authzmanager.controller.admin.clients.carrier;

import jakarta.validation.constraints.NotNull;
import me.sonam.authzmanager.controller.admin.roles.Role;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.UUID;

public class ClientOrganizationUserWithRole {
    private static final Logger LOG = LoggerFactory.getLogger(ClientOrganizationUserWithRole.class);
    private UUID id; //row id

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    private UUID clientId;
    private UUID organizationId;
    public User user;
    @NotNull
    private Role role;

    public ClientOrganizationUserWithRole() {
    }

    public ClientOrganizationUserWithRole(UUID clientId, UUID organizationId, User user, Role role) {
        this.clientId = clientId;
        this.organizationId = organizationId;
        this.user = user;
        this.role = role;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public boolean equals(Object object) {
        LOG.info("Hello");
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) {
            LOG.info("return false object being null");
            return false;
        }

        ClientOrganizationUserWithRole that = (ClientOrganizationUserWithRole) object;
        return Objects.equals(user.getId(), that.user.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(user.getId());
    }

    @Override
    public String toString() {
        return "ClientOrganizationUserWithRole{" +
                "id=" + id +
                ", clientId=" + clientId +
                ", organizationId=" + organizationId +
                ", user=" + user +
                '}';
    }
}
