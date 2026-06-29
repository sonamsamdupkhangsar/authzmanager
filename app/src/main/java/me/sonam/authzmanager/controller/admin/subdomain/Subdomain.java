package me.sonam.authzmanager.controller.admin.subdomain;

import java.time.LocalDateTime;
import java.util.UUID;

public class Subdomain {
    private UUID id;
    private String host;
    private LocalDateTime created;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }
}
