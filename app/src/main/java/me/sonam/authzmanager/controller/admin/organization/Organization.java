package me.sonam.authzmanager.controller.admin.organization;

import java.util.UUID;

/**
 * this is Organization class used for entering data into organization-rest-service
 */
public class Organization {
    private UUID id;
    private String name;
    private UUID creatorUserId;

    public Organization() {

    }

    public Organization(UUID id, String name, UUID creatorUserId) {
        this.id = id;
        this.name = name;
        this.creatorUserId = creatorUserId;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UUID getCreatorUserId() {
        return creatorUserId;
    }

    @Override
    public String toString() {
        return "Organization{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", creatorUserId=" + creatorUserId +
                '}';
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setCreatorUserId(UUID creatorUserId) {
        this.creatorUserId = creatorUserId;
    }
}
