package me.sonam.authzmanager.controller.admin.subdomain;

import me.sonam.authzmanager.clients.user.User;
import me.sonam.authzmanager.controller.admin.organization.Organization;
import me.sonam.authzmanager.rest.RestPage;

import java.util.List;
import java.util.UUID;

public record SubdomainOrganizationUsers(Organization organization, RestPage<UUID> userIdPage, List<User> users) {
}
