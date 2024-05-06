package me.sonam.authzmanager.controller.clients;

import me.sonam.authzmanager.webclients.ClientOrganizationWebClient;
import me.sonam.authzmanager.webclients.OauthClientWebClient;
import me.sonam.authzmanager.webclients.OrganizationWebClient;
import me.sonam.authzmanager.clients.user.ClientOrganization;
import me.sonam.authzmanager.controller.admin.oauth2.OauthClient;
import me.sonam.authzmanager.controller.admin.oauth2.RegisteredClient;
import me.sonam.authzmanager.controller.admin.organization.Organization;
import me.sonam.authzmanager.user.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
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

    public ClientOrganizationController(ClientOrganizationWebClient clientOrganizationWebClient,
                                        OrganizationWebClient organizationWebClient,
                                        OauthClientWebClient oauthClientWebClient) {
        this.clientOrganizationWebClient = clientOrganizationWebClient;
        this.organizationWebClient = organizationWebClient;
        this.oauthClientWebClient = oauthClientWebClient;
    }

    
    /**
     * id is the {@link RegisteredClient#getId()} field not the clientId
     * id is the client.id (not clientId)
     */

    @PostMapping("client/id/{id}/organizations")
    public Mono<String> addOrganizationToClient(@PathVariable("id") UUID id, ClientOrganization clientOrganization,
                                                Model model, final Pageable userPageable) {
        LOG.info("add organization to clientId: {}",clientOrganization);
        final String PATH = "/admin/clients/organizations";

        Pageable pageable = PageRequest.of(userPageable.getPageNumber(), 5, Sort.by("name"));
        UserId userId = (UserId) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return clientOrganizationWebClient.addClientToOrganization(clientOrganization.getClientId(),
                        clientOrganization.getOrganizationId())
                .doOnNext(s -> model.addAttribute("message", "client has been successfully added to organization"))
                .flatMap(organizationRestPage -> setClientInModel(id, model, PATH))
                .flatMap(s -> organizationWebClient.getOrganizationPageByOwner(userId.getUserId(), pageable))
                .doOnNext(restPage -> {
                    LOG.info("organizationList: {}", restPage);
                    model.addAttribute("page", restPage);
                })
                .flatMap(organizationRestPage -> {
                    List<ClientOrganization> clientOrganizationList = new ArrayList<>();

                    return clientOrganizationWebClient.getClientIdOrganizationIdMatch(organizationRestPage.getContent(), id)
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
    @DeleteMapping("client/id/{id}/organizations/id/{organizationId}")
    public Mono<String> deleteClientOrganizationAssociation(@PathVariable("id") UUID clientsId, @PathVariable("organizationId")UUID organizationId,
                                                            Model model,
                                                            final Pageable userPageable) {
        LOG.info("delete client organization association");
        final String PATH = "/admin/clients/organizations";

        Pageable pageable = PageRequest.of(userPageable.getPageNumber(), 5, Sort.by("name"));
        UserId userId = (UserId) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return clientOrganizationWebClient.deleteClientOrganizationAssociation(clientsId, organizationId)
                .doOnNext(s -> model.addAttribute("message", "client has been successfully removed from organization"))
                .flatMap(organizationRestPage -> setClientInModel(clientsId, model, PATH))
                .flatMap(s -> organizationWebClient.getOrganizationPageByOwner(userId.getUserId(), pageable))
                .doOnNext(restPage -> {
                    LOG.info("organizationList: {}", restPage);
                    model.addAttribute("page", restPage);
                })
                .flatMap(organizationRestPage -> {
                    List<ClientOrganization> clientOrganizationList = new ArrayList<>();

                    return clientOrganizationWebClient.getClientIdOrganizationIdMatch(organizationRestPage.getContent(), clientsId)
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


    private Mono<String> setClientInModel(UUID id, Model model, final String PATH) {
        return oauthClientWebClient.getOauthClientById(id).map(registeredClient -> {
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
