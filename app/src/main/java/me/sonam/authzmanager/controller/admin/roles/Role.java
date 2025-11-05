package me.sonam.authzmanager.controller.admin.roles;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * this is Organization class used for entering data into organization-rest-service
 */
public class Role {
    private UUID id;
    @NotEmpty(message = "name cannot be empty")
    @Size(min = 3, max = 50)
    private String name;
    private UUID organizationId;

    public Role() {
    }

    public Role(UUID id, String name, UUID organizationId) {
        this.id = id;
        this.name = name;
        this.organizationId = organizationId;

    }
    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", organizationId=" + organizationId +
                '}';
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(UUID id) {
        this.id = id;
    }

}
