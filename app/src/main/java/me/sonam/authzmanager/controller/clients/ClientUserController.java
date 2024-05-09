package me.sonam.authzmanager.controller.clients;

import me.sonam.authzmanager.webclients.RoleWebClient;
import me.sonam.authzmanager.controller.clients.carrier.ClientOrganizationUserWithRole;
import me.sonam.authzmanager.user.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * this controller is for setting the Client Organization User Role
 */
@Controller
@RequestMapping("/admin/clients/{id}/users")
public class ClientUserController {
    private static final Logger LOG = LoggerFactory.getLogger(ClientUserController.class);
    private RoleWebClient roleWebClient;
    private ClientUserPage clientUserPage;

    public ClientUserController(RoleWebClient roleWebClient, ClientUserPage clientUserPage) {
        this.roleWebClient = roleWebClient;
        this.clientUserPage = clientUserPage;
    }

    @PostMapping("/role")
    public Mono<String> addOrganizationToClient(@PathVariable("id")UUID id, ClientOrganizationUserWithRole clientOrganizationUserWithRole,
                                                Model model, final Pageable userPageable) {
        LOG.info("client.id: {}", id);
        LOG.info("add organization to clientId: {}", clientOrganizationUserWithRole);
        final String PATH = "/admin/clients/users";
        UserId userId = (UserId) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        LOG.info("userId: {}", userId);

        return roleWebClient.addClientOrganizationUserRole(clientOrganizationUserWithRole)
                .doOnNext(clientOrganizationUserRole -> LOG.info("saved client organization role"))
                .flatMap(clientOrganizationUserRole ->
                        clientUserPage.setUsersAndsersInClientOrganizationUserRole(
                                clientOrganizationUserRole.getClientId(), model, userPageable))
                .thenReturn(PATH);
    }

    @DeleteMapping("/client-organization-user-role/{roleId}")
    public Mono<String> deleteClientOrganizationUserRole(@PathVariable("id")UUID clientsId, @PathVariable("roleId") UUID roleId, Model model, Pageable userPageable) {
        LOG.info("delete client organization user role by id: {} in client.id: {}", roleId, clientsId);
        final String PATH = "/admin/clients/users";

        return roleWebClient.deleteClientOrganizationUserRole(roleId)
                        .flatMap(s -> {
                            LOG.info("response: {}", s);
                           return clientUserPage.setUsersAndsersInClientOrganizationUserRole(clientsId, model, userPageable);
                        }).thenReturn(PATH);
    }
}
