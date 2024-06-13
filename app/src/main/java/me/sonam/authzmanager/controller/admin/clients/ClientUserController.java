package me.sonam.authzmanager.controller.admin.clients;

import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.RoleWebClient;
import me.sonam.authzmanager.controller.admin.clients.carrier.ClientOrganizationUserWithRole;
import me.sonam.authzmanager.user.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
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
    private TokenService tokenService;

    public ClientUserController(RoleWebClient roleWebClient, ClientUserPage clientUserPage, TokenService tokenService) {
        this.roleWebClient = roleWebClient;
        this.clientUserPage = clientUserPage;
        this.tokenService = tokenService;
    }

    @PostMapping("/role")
    public Mono<String> addOrganizationToClient(@PathVariable("id")UUID id, ClientOrganizationUserWithRole clientOrganizationUserWithRole,
                                                Model model, final Pageable userPageable) {
        LOG.info("client.id: {}", id);
        LOG.info("add organization to clientId: {}", clientOrganizationUserWithRole);
        final String PATH = "/admin/clients/users";

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String accessToken = tokenService.getAccessToken(authentication).getTokenValue();

        return roleWebClient.addClientOrganizationUserRole(accessToken, clientOrganizationUserWithRole)
                .doOnNext(clientOrganizationUserRole -> LOG.info("saved client organization role"))
                .flatMap(clientOrganizationUserRole ->
                        clientUserPage.setUsersAndsersInClientOrganizationUserRole(accessToken,
                                clientOrganizationUserRole.getClientId(), model, userPageable))
                .thenReturn(PATH);
    }

    @DeleteMapping("/client-organization-user-role/{roleId}")
    public Mono<String> deleteClientOrganizationUserRole(@PathVariable("id")UUID clientsId, @PathVariable("roleId") UUID roleId, Model model, Pageable userPageable) {
        LOG.info("delete client organization user role by id: {} in client.id: {}", roleId, clientsId);
        final String PATH = "/admin/clients/users";

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String accessToken = tokenService.getAccessToken(authentication).getTokenValue();

        return roleWebClient.deleteClientOrganizationUserRole(accessToken, roleId)
                        .flatMap(s -> {
                            LOG.info("response: {}", s);
                           return clientUserPage.setUsersAndsersInClientOrganizationUserRole(accessToken, clientsId, model, userPageable);
                        }).thenReturn(PATH);
    }
}
