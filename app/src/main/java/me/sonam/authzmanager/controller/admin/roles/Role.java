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

    private UUID userId;
    public Role() {

    }

    public Role(UUID id, String name, UUID userId) {
        this.id = id;
        this.name = name;
        this.userId = userId;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UUID getUserId() {
        return this.userId;
    }
    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(UUID id) {
        this.id = id;
    }

}
