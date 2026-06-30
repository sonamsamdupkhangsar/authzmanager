package me.sonam.authzmanager.controller.admin.subdomain;

import me.sonam.authzmanager.clients.user.User;

public record SubdomainUserRow(SubdomainOrganizationUser membership, User user) {
}
