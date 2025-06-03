package me.sonam.authzmanager.controller.admin.clients;

import me.sonam.authzmanager.AuthzManagerException;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.ClientOrganizationWebClient;
import me.sonam.authzmanager.webclients.OauthClientWebClient;
import me.sonam.authzmanager.webclients.OrganizationWebClient;
import me.sonam.authzmanager.clients.user.ClientOrganization;
import me.sonam.authzmanager.oauth2.OauthClient;
import me.sonam.authzmanager.oauth2.RegisteredClient;
import me.sonam.authzmanager.controller.admin.organization.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
@Controller
@RequestMapping("/admin/clients/organizations")
public class ClientOrganizationController {
    private static final Logger LOG = LoggerFactory.getLogger(ClientOrganizationController.class);
    private ClientOrganizationWebClient clientOrganizationWebClient;
    private OrganizationWebClient organizationWebClient;
    private OauthClientWebClient oauthClientWebClient;
    private TokenService tokenService;

    public ClientOrganizationController(ClientOrganizationWebClient clientOrganizationWebClient,
                                        OrganizationWebClient organizationWebClient,
                                        OauthClientWebClient oauthClientWebClient,
                                        TokenService tokenService) {
        this.clientOrganizationWebClient = clientOrganizationWebClient;
        this.organizationWebClient = organizationWebClient;
        this.oauthClientWebClient = oauthClientWebClient;
        this.tokenService = tokenService;
    }

    
    /**
     * id is the {@link RegisteredClient#getId()} field not the clientId
     * id is the client.id (not clientId)
     */

    @PostMapping(path = "client/{id}/organizations")
    public Mono<String> addOrganizationToClient(ClientOrganization clientOrganization, @PathVariable("id") UUID id,
                                                Model model, final Pageable userPageable) {
        LOG.info("add organization to clientId: {}",clientOrganization);
        final String PATH = "/admin/clients/organizations";

        Pageable pageable = PageRequest.of(userPageable.getPageNumber(), 5, Sort.by("name"));

        if (clientOrganization.getClientId() == null || clientOrganization.getOrganizationId() == null) {
            LOG.error("clientId or organizationId in payload is null: {}", clientOrganization);
            return Mono.error(new AuthzManagerException("clientOrganization.clientId or clientOrganization.organizationId cannot be null"));
        }

        DefaultOidcUser oidcUser = (DefaultOidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID userId = UUID.fromString(oidcUser.getAttribute("userId"));
        LOG.info("userId: {}", userId);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String accessToken = tokenService.getAccessToken();//authentication).getTokenValue();

        return clientOrganizationWebClient.addClientToOrganization(accessToken, clientOrganization.getClientId(),
                        clientOrganization.getOrganizationId())
                .doOnNext(s -> model.addAttribute("message", "client has been successfully added to organization"))
                .flatMap(organizationRestPage -> setClientInModel(accessToken, id, model, PATH))
                .flatMap(s -> organizationWebClient.getOrganizationPageByOwner(accessToken, userId, pageable))
                .doOnNext(restPage -> {
                    LOG.info("organizationList: {}", restPage);
                    model.addAttribute("page", restPage);
                })
                .flatMap(organizationRestPage -> {
                    List<ClientOrganization> clientOrganizationList = new ArrayList<>();

                    return clientOrganizationWebClient.getClientIdOrganizationIdMatch(accessToken, organizationRestPage.getContent(), id)
                            .switchIfEmpty(Mono.just(new ClientOrganization()))
                            .doOnNext(clientOrganizationResponse -> {
                                for (Organization organization : organizationRestPage.getContent()) {
                                    if (id.equals(clientOrganizationResponse.getClientId()) &&
                                            organization.getId().equals(clientOrganizationResponse.getOrganizationId())) {
                                        ClientOrganization clientOrganization1 = new ClientOrganization(id, organization, true);
                                        LOG.info("found a match and add the clientOrganization to list");
                                        clientOrganizationList.add(clientOrganization1);
                                    } else {
                                        ClientOrganization clientOrganization1 = new ClientOrganization(id, organization, false);
                                        LOG.info("add clientOrganization to list");
                                        clientOrganizationList.add(clientOrganization1);
                                    }
                                }
                                LOG.info("add clientOrganizations as modelAttribute: {}", clientOrganizationList);
                                model.addAttribute("clientOrganizations", clientOrganizationList);
                            });
                })
                .then(Mono.just(PATH));
    }
    @DeleteMapping("client/{id}/organizations/{organizationId}")
    public Mono<String> deleteClientOrganizationAssociation(@PathVariable("id") UUID clientsId, @PathVariable("organizationId")UUID organizationId,
                                                            Model model,
                                                            final Pageable userPageable) {
        LOG.info("delete client organization association");
        final String PATH = "/admin/clients/organizations";

        Pageable pageable = PageRequest.of(userPageable.getPageNumber(), 5, Sort.by("name"));
        DefaultOidcUser oidcUser = (DefaultOidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID userId = UUID.fromString(oidcUser.getAttribute("userId"));
        LOG.info("userId: {}", userId);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String accessToken = tokenService.getAccessToken();//authentication).getTokenValue();


        return clientOrganizationWebClient.deleteClientOrganizationAssociation(accessToken, clientsId, organizationId)
                .doOnNext(s -> model.addAttribute("message", "client has been successfully removed from organization"))
                .flatMap(organizationRestPage -> setClientInModel(accessToken, clientsId, model, PATH))
                .flatMap(s -> organizationWebClient.getOrganizationPageByOwner(accessToken, userId, pageable))
                .doOnNext(restPage -> {
                    LOG.info("organizationList: {}", restPage);
                    model.addAttribute("page", restPage);
                })
                .flatMap(organizationRestPage -> {
                    List<ClientOrganization> clientOrganizationList = new ArrayList<>();

                    return clientOrganizationWebClient.getClientIdOrganizationIdMatch(accessToken, organizationRestPage.getContent(), clientsId)
                            .switchIfEmpty(Mono.just(new ClientOrganization()))
                            .doOnNext(clientOrganizationResponse -> {
                                for (Organization organization : organizationRestPage.getContent()) {
                                    if (clientsId.equals(clientOrganizationResponse.getClientId()) &&
                                            organization.getId().equals(clientOrganizationResponse.getOrganizationId())) {
                                        ClientOrganization clientOrganization1 = new ClientOrganization(clientsId, organization, true);
                                        LOG.info("found a match and add the clientOrganization to list");
                                        clientOrganizationList.add(clientOrganization1);
                                    } else {
                                        ClientOrganization clientOrganization1 = new ClientOrganization(clientsId, organization, false);
                                        LOG.info("add clientOrganization to list");
                                        clientOrganizationList.add(clientOrganization1);
                                    }
                                }
                                LOG.info("add clientOrganizations as modelAttribute: {}", clientOrganizationList);
                                model.addAttribute("clientOrganizations", clientOrganizationList);
                            });
                })
                .then(Mono.just(PATH));
    }


    private Mono<String> setClientInModel(String accessToken, UUID id, Model model, final String PATH) {
        return oauthClientWebClient.getOauthClientById(accessToken, id).map(registeredClient -> {
            LOG.info("got client {}", registeredClient);
            try {
                OauthClient oauthClient = OauthClient.getFromRegisteredClient(registeredClient);
                LOG.info("oauthClient {}", oauthClient.getClientAuthenticationMethods());
                model.addAttribute("client", oauthClient);

            } catch (Exception e) {
                LOG.error("failed to parse to OautClient", e);
                model.addAttribute("client", new OauthClient());

            }
            return PATH;
        }).onErrorResume(throwable -> {
            LOG.error("Failed to get client by clientId", throwable);
            model.addAttribute("client", new OauthClient());

            return null;
        });
    }
}
