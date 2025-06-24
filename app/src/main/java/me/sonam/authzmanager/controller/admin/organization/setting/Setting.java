package me.sonam.authzmanager.controller.admin.organization.setting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Setting {
    private static final Logger LOG = LoggerFactory.getLogger(Setting.class);

    private String defaultOrganizationId;

    public Setting() {

    }

    public Setting(String defaultOrganizationId) {
        this.defaultOrganizationId = defaultOrganizationId;
        LOG.info("defaultOrganizationId: {}", defaultOrganizationId);
    }

    public String getDefaultOrganizationId() {
        return this.defaultOrganizationId;
    }

    public void setDefaultOrganizationId(String organizationId) {
        this.defaultOrganizationId = organizationId;
    }

    @Override
    public String toString() {
        return "Setting{" +
                "defaultOrganizationId='" + defaultOrganizationId + '\'' +
                '}';
    }
}
