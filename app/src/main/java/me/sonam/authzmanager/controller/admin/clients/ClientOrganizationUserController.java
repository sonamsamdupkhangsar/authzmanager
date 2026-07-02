package me.sonam.authzmanager.controller.admin.clients;

import jakarta.validation.Valid;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.RoleWebClient;
import me.sonam.authzmanager.controller.admin.clients.carrier.ClientOrganizationUserWithRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * this controller is for setting the Client Organization User Role
 */
@Controller
@RequestMapping("/admin/clients/{clientId}/organizations/users/roles")
public class ClientOrganizationUserController {
    private static final Logger LOG = LoggerFactory.getLogger(ClientOrganizationUserController.class);
    private RoleWebClient roleWebClient;
    private ClientUserPage clientUserPage;
    private TokenService tokenService;

    public ClientOrganizationUserController(RoleWebClient roleWebClient, ClientUserPage clientUserPage, TokenService tokenService) {
        this.roleWebClient = roleWebClient;
        this.clientUserPage = clientUserPage;
        this.tokenService = tokenService;
    }

    @PostMapping
    public Mono<String> addOrganizationToClient(@PathVariable("clientId")UUID id, @Valid ClientOrganizationUserWithRole clientOrganizationUserWithRole,
                                                BindingResult bindingResult, Model model, final Pageable userPageable) {
        LOG.info("Add user role for client with organization: {}", clientOrganizationUserWithRole);
        final String PATH = "/admin/clients/users";

        if (bindingResult.hasErrors()) {
            LOG.info("user didn't enter required fields: {}, errors: {}", bindingResult.getFieldError(), bindingResult.getAllErrors());
            model.addAttribute("error", "Data validation failed");
            return Mono.just(PATH);
        }


        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String accessToken = tokenService.getAccessToken();//authentication).getTokenValue();

        return roleWebClient.addClientOrganizationUserRole(accessToken, clientOrganizationUserWithRole)
                .doOnNext(clientOrganizationUserRole -> LOG.info("saved client organization role"))
                .flatMap(clientOrganizationUserRole ->
                        clientUserPage.setUsersAndsersInClientOrganizationUserRole(accessToken,
                                clientOrganizationUserRole.getClientId(), model, userPageable))
                .thenReturn(PATH);
    }

    @DeleteMapping("/{clientOrganizationUserRoleId}")
    public Mono<String> deleteClientOrganizationUserRole(@PathVariable("clientId")UUID clientsId, @PathVariable("clientOrganizationUserRoleId") UUID id, Model model, final Pageable userPageable) {
        LOG.info("delete client organization user role by id: {} in client.id: {}", id, clientsId);
        final String PATH = "/admin/clients/users";

        String accessToken = tokenService.getAccessToken();

        LOG.info("authenticated client-organization role deletion requested");
        return roleWebClient.deleteClientOrganizationUserRole(accessToken, id)
                        .flatMap(s -> {
                            LOG.info("response: {}", s);
                           return clientUserPage.setUsersAndsersInClientOrganizationUserRole(accessToken, clientsId, model, userPageable);
                        }).thenReturn(PATH);
    }
}
