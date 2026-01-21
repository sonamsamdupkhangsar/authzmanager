package me.sonam.authzmanager.controller.admin.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import me.sonam.authzmanager.clients.user.ClientOrganization;
import me.sonam.authzmanager.clients.user.User;
import me.sonam.authzmanager.controller.admin.clients.carrier.ClientOrganizationUserWithRole;
import me.sonam.authzmanager.controller.admin.organization.Organization;
import me.sonam.authzmanager.oauth2.AuthorizationGrantType;
import me.sonam.authzmanager.oauth2.OauthClient;
import me.sonam.authzmanager.oauth2.RegisteredClient;
import me.sonam.authzmanager.oauth2.util.RegisteredClientUtil;
import me.sonam.authzmanager.rest.CustomPair;
import me.sonam.authzmanager.rest.RestPage;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.*;

@Controller
@RequestMapping("/admin/clients")
public class ClientController implements ClientUserPage {
    private static final Logger LOG = LoggerFactory.getLogger(ClientController.class);

    private OrganizationWebClient organizationWebClient;
    private OauthClientWebClient oauthClientWebClient;
    private ClientOrganizationWebClient clientOrganizationWebClient;
    private UserWebClient userWebClient;
    private RoleWebClient roleWebClient;

    private RegisteredClientUtil registeredClientUtil = new RegisteredClientUtil();

    private TokenService tokenService;
    @Value("${maxClients}")
    private int maxClients;

    public ClientController(OauthClientWebClient oauthClientWebClient,
                            OrganizationWebClient organizationWebClient,
                            ClientOrganizationWebClient clientOrganizationWebClient,
                            UserWebClient userWebClient, RoleWebClient roleWebClient,
                            TokenService tokenService) {
        this.oauthClientWebClient = oauthClientWebClient;
        this.organizationWebClient = organizationWebClient;
        this.clientOrganizationWebClient = clientOrganizationWebClient;
        this.userWebClient = userWebClient;
        this.roleWebClient = roleWebClient;
        this.tokenService = tokenService;
    }

    @GetMapping("/createForm")
    public String getCreateForm(Model model) {
        LOG.info("return createForm");
        final String PATH = "admin/clients/form";

        OauthClient oauthClient = new OauthClient();
        oauthClient.setClientIdUuid(UUID.randomUUID());

        model.addAttribute("client", oauthClient);
        return PATH;
    }

    //id is a UUID
    @GetMapping("/{id}")
    public Mono<String> getClientByClientId(@PathVariable("id") UUID id, Model model) {
        LOG.info("get client by id {}", id);
        final String PATH = "admin/clients/form";
        String accessToken = tokenService.getAccessToken();

        return setClientInModel(accessToken, id, model, PATH).thenReturn(PATH);
    }

    private Mono<String> setClientInModel(String accessToken, UUID id, Model model, final String PATH) {
        LOG.info("set client in model with accesssToken: {}", accessToken);

        return oauthClientWebClient.getOauthClientById(accessToken, id).map(registeredClient -> {
            LOG.info("got client {}", registeredClient);
            try {
                OauthClient oauthClient = OauthClient.getFromRegisteredClient(registeredClient);
                LOG.info("grantTypes {}", oauthClient.getAuthorizationGrantTypes());
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

    /**
     * This will handle the client creation and client update.
     *
     * @param client
     * @param bindingResult
     * @param model
     * @return
     */
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<String> updateClient(@RequestBody @Valid @ModelAttribute("client") OauthClient client, BindingResult bindingResult, Model model) {

        LOG.info("update client");
        LOG.info("newClientSecret: {}", client.getNewClientSecret());
        final String PATH = "admin/clients/form";

        String accessToken = tokenService.getAccessToken();

        DefaultOidcUser defaultOidcUser = (DefaultOidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userIdString = defaultOidcUser.getAttribute("userId");

        return hasDataValidationError(client, PATH, bindingResult, model)
            .flatMap(aBoolean -> getRegisteredClient(client))
            .flatMap(registeredClient -> {
                if (registeredClient.getId() == null) {
                    LOG.info("a new client to create, check if max count of clients reached");
                    return oauthClientWebClient.getClientCount(accessToken).zipWith(Mono.just(registeredClient));
                }
                return Mono.just(0).zipWith(Mono.just(registeredClient));
            })
                .flatMap(objects -> {
                    if (objects.getT1() <= maxClients) {
                        RegisteredClient registeredClient = objects.getT2();
                        Map<String, Object> map = registeredClientUtil.getMapObject(registeredClient);
                        map.put("userId", userIdString);

                        LOG.info("map is {}", map);
                        LOG.info("clientIdIssuedAt: {}", map.get("clientIdIssuedAt"));

                        return Mono.just(map);
                    }
                    return Mono.error(new BadRequestException("max client count reached"));
                })
                .flatMap(map -> oauthClientWebClient.updateClient(accessToken, map))
                .flatMap(updatedRegisteredClient -> {
                    LOG.info("client updated and registeredClient returned");
                    LOG.info("updatedRegisteredClient.clientIdIssuedAt: {}", updatedRegisteredClient.getClientIdIssuedAt());

                    OauthClient oauthClient = OauthClient.getFromRegisteredClient(updatedRegisteredClient);
                    LOG.info("oauthClient {}", oauthClient);

                    model.addAttribute("client", oauthClient);
                    LOG.info("returning to path: {}", PATH);

                    if (client.getId() == null || client.getId().isEmpty()) {
                        model.addAttribute("message", "Client created successfully!");
                    }
                    else {
                        model.addAttribute("message", "Client updated!");
                    }
                    return Mono.just(PATH);
            }).onErrorResume(throwable -> {
                checkIfOauth2ClientError(bindingResult, (Exception) throwable);

                LOG.error("Failed to update client {}", throwable.getMessage());
                model.addAttribute("error", "Failed");

                if (throwable instanceof WebClientResponseException) {
                    WebClientResponseException webClientResponseException = (WebClientResponseException) throwable;
                    LOG.error("error body contains: {}", webClientResponseException.getResponseBodyAsString());
                    model.addAttribute("error", webClientResponseException.getResponseBodyAsString());
                }
                return Mono.just(PATH);
            });
    }

    private Mono<RegisteredClient> getRegisteredClient(OauthClient client) {
            if (client.getId() == null || client.getId().isEmpty()) {
                client.setTokenSettings(null);
                client.setClientSettings(null);
                LOG.info("it's a create client");
            }
            else {
                LOG.info("client.id is not null and not empty, it's an update");
            }
            return Mono.just(client.getRegisteredClient());
    }

    private Mono<Boolean> hasDataValidationError(OauthClient client, String PATH, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            LOG.info("user didn't enter required fields: {}, errors: {}", bindingResult.getFieldError(), bindingResult.getAllErrors());
            model.addAttribute("error", "Data validation failed");
            return Mono.error(new BadRequestException("data validation error"));
        }

        LOG.info("client.getAuthorizationGrantTypes: {}", client.getAuthorizationGrantTypes());

        if (client.getAuthorizationGrantTypes().contains(AuthorizationGrantType.AUTHORIZATION_CODE.getValue().toUpperCase())) {
            LOG.info("authorizationGrantTypes contains Authorization_Code");
            if (client.getRedirectUris().trim().isEmpty()) {
                final String error = "redirect uris is needed for AuthorizationGrantType of AUTHORIZATION_CODE";
                LOG.error(error);
                bindingResult.rejectValue("redirectUris", "error.user", error);

                return Mono.error(new BadRequestException("data validation error"));
            }
        }
        return Mono.just(false);
    }

    private void checkIfOauth2ClientError(BindingResult bindingResult, Exception e) {
        if (e.getMessage().startsWith("authorizationCodeTimeToLive")) {
            bindingResult.rejectValue("tokenSettings.authorizationCodeTimeToLive", "error.user", "authorizationCodeTimeToLive value must be greater than 0");
        } else if (e.getMessage().startsWith("accessTokenTimeToLive")) {
            bindingResult.rejectValue("tokenSettings.accessTokenTimeToLive", "error.user", "accessTokenTimeToLive value must be greater than 0");
        } else if (e.getMessage().startsWith("deviceCodeTimeToLive")) {
            bindingResult.rejectValue("tokenSettings.deviceCodeTimeToLive", "error.user", "deviceCodeTimeToLive value must be greater than 0");
        } else if (e.getMessage().startsWith("refreshTokenTimeToLive")) {
            bindingResult.rejectValue("tokenSettings.refreshTokenTimeToLive", "error.user", "refreshTokenTimeToLive value must be greater than 0");
        }
    }

    @GetMapping
    public Mono<String> getLoggedInUserClients(Model model, Pageable userPageable) {
        LOG.info("get this logged-in users clients only");
        int pageSize = 5;

        if (userPageable.getPageSize() < 100) {
            pageSize = userPageable.getPageSize();
            LOG.info("taking page size from pageable: {}", pageSize);
        }

        Pageable pageable = PageRequest.of(userPageable.getPageNumber(), pageSize, Sort.by("name"));

        final String PATH = "/admin/clients/list";
        LOG.info("principal: {}", SecurityContextHolder.getContext().getAuthentication().getPrincipal());

        DefaultOidcUser oidcUser = (DefaultOidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID userId = UUID.fromString(oidcUser.getAttribute("userId"));
        LOG.info("userId: {}", userId);

        String accessToken = tokenService.getAccessToken();

        return oauthClientWebClient.getUserClientIds(accessToken, userId, pageable).flatMap(page -> {
            LOG.info("got clientIds for this userId: {}", userId);
            model.addAttribute("page", page);
            allowCreateClient(page, model);

            return Mono.just(PATH);
        }).onErrorResume(throwable -> {
            LOG.error("error occued on calling get user clientIds: {}", throwable);
            model.addAttribute("message", "failed");
            return Mono.just(PATH);
        });
    }
    @DeleteMapping("{id}")
    public Mono<String> deleteClientById(@PathVariable("id") UUID id, Model model) {
        LOG.info("delete client by id: {}", id);
        final String PATH = "/admin/clients/list";

        DefaultOidcUser oidcUser = (DefaultOidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID userId = UUID.fromString(oidcUser.getAttribute("userId"));
        LOG.info("userId: {}", userId);

        String accessToken = tokenService.getAccessToken();


        return oauthClientWebClient.deleteClient(accessToken, id, userId)
                .thenReturn(PATH)
                .onErrorResume(throwable -> {
                    LOG.error("error occurred on deleting client by id: {}", id, throwable);
                    model.addAttribute("error", "Failed to delete by clientId");
                    return Mono.just(PATH);
                });

    }

    /**
     * Get organizations created by the logged-in user
     *
     * @param model
     * @param userPageable
     * @return
     */

    @GetMapping("{id}/organizations")
    public Mono<String> getOrganizations(
            @PathVariable("id") UUID id, Model model, Pageable userPageable) {
        LOG.info("get organizations created or owned by this user-id: {}", id);
        final String PATH = "/admin/clients/organizations";
        int pageSize = 5;

        if (userPageable.getPageSize() < 100) {
            pageSize = userPageable.getPageSize();
            LOG.info("taking page size from pageable: {}", pageSize);
        }

        Pageable pageable = PageRequest.of(userPageable.getPageNumber(), pageSize, Sort.by("name"));
        DefaultOidcUser oidcUser = (DefaultOidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userIdString = oidcUser.getAttribute("userId");
        LOG.info("oidc.userId: {}", userIdString);
        UUID userId = UUID.fromString(userIdString);


        String accessToken = tokenService.getAccessToken();

        return setClientInModel(accessToken, id, model, PATH)
                .flatMap(s -> organizationWebClient.getOrganizationPageByOwner(accessToken, userId, pageable))
                .doOnNext(restPage -> {
                    LOG.info("organizationList: {}", restPage);
                    LOG.info("print organization rest page as json: {}", getJson(restPage));

                    model.addAttribute("page", restPage);
                }).flatMap(organizationRestPage -> {
                    List<ClientOrganization> clientOrganizationList = new ArrayList<>();

                    return clientOrganizationWebClient.getClientIdOrganizationIdMatch(accessToken, organizationRestPage.content(), id)
                            .switchIfEmpty(Mono.just(new ClientOrganization()))
                            .doOnNext(clientOrganization -> {
                                for (Organization organization : organizationRestPage.content()) {
                                    if (id.equals(clientOrganization.getClientId()) &&
                                            organization.getId().equals(clientOrganization.getOrganizationId())) {
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
                }).thenReturn(PATH);
    }

    /**
     * When user clicks on the User tab in Clients page
     * get users in the clients' organization.
     *
     * @param id
     * @param model
     * @param userPageable
     * @return
     */
    @GetMapping("{id}/users")
    public Mono<String> getUsers(@PathVariable("id") UUID id, Model model, Pageable userPageable) {
        String accessToken = tokenService.getAccessToken();

        return setUsersAndsersInClientOrganizationUserRole(accessToken, id, model, userPageable);
    }

    @Override
    public Mono<String> setUsersAndsersInClientOrganizationUserRole(String accessToken, UUID id, Model model, Pageable userPageable) {
        LOG.info("get client users relationships");
        final String PATH = "/admin/clients/users";
        int pageSize = 5;

        if (userPageable.getPageSize() < 100) {
            pageSize = userPageable.getPageSize();
            LOG.info("taking page size from pageable: {}", pageSize);
        }

        Pageable pageable = PageRequest.of(userPageable.getPageNumber(), pageSize, Sort.by("name"));


        return setClientInModel(accessToken, id, model, PATH)
                .flatMap(s -> clientOrganizationWebClient.getOrganizationIdAssociatedWithClientId(accessToken, id))
                .flatMap(uuid -> organizationWebClient.getOrganizationById(accessToken, uuid))
                .doOnNext(organization -> model.addAttribute("organization", organization))
                .flatMap(organization -> {
                    LOG.info("get roles");
                    return roleWebClient.getRolesByOrganizationId(accessToken, organization.getId(), PageRequest.of(0, Integer.MAX_VALUE))
                            .zipWith(Mono.just(organization));
                })
                .doOnNext(objects -> {
                    LOG.info("got roles: {}", objects.getT1().content());
                    model.addAttribute("roles", objects.getT1().content());
                }) //objects = roles, organizationId
                .flatMap(objects -> organizationWebClient.getUserIdsInOrganizationId(accessToken, objects.getT2().getId(), pageable).zipWith(Mono.just(objects.getT2())))
                .flatMap(objects -> {
                    LOG.info("uuidPage: {}", objects.getT1().content());
                    model.addAttribute("page", objects.getT1());
                    return userWebClient.getUserByBatchOfIds(accessToken, objects.getT1().content()).zipWith(Mono.just(objects.getT2()));
                })
                .doOnNext(objects -> {//objects = users, organization
                    LOG.info("got users: {}", objects.getT1());
                    //  model.addAttribute("users", objects.getT1());
                })
                //find users that have this clientId with a role
                .flatMap(objects -> {
                    List<UUID> userIds = objects.getT1().stream().map(User::getId).toList();
                    return roleWebClient.getClientOrganizationUserWithRoles(accessToken, id, objects.getT2().getId(), userIds)
                            .switchIfEmpty(Mono.just(new ArrayList<ClientOrganizationUserWithRole>()))
                            .zipWith(Mono.just(objects.getT1()));
                })
                .doOnNext(objects -> { // objects = ClientOrganizationUserWithRole,users
                    List<User> usersInOrganizationList = objects.getT2();
                    LOG.info("clientOrganizationUsreWithRoles: {}", objects.getT1());

                    List<ClientOrganizationUserWithRole> clientOrganizationUserWithRoleList = objects.getT1();

                    Map<UUID, User> userMap = new HashMap<>();
                    List<User> userList = usersInOrganizationList;
                    userList.forEach(user -> userMap.put(user.getId(), user));
                    LOG.info("userMap contains: {}", userMap);


                    List<ClientOrganizationUserWithRole> userInClientOrganizationRoleList = clientOrganizationUserWithRoleList.stream().map(clientOrganizationUserWithRole -> {
                        User user = userMap.get(clientOrganizationUserWithRole.getUser().getId());
                        LOG.info("get from map with user.id: {}", clientOrganizationUserWithRole.getUser().getId());
                        clientOrganizationUserWithRole.getUser().setFirstName(user.getFirstName());
                        clientOrganizationUserWithRole.getUser().setLastName(user.getLastName());
                        clientOrganizationUserWithRole.getUser().setEmail(user.getEmail());
                        clientOrganizationUserWithRole.getUser().setAuthenticationId(user.getAuthenticationId());
                        return clientOrganizationUserWithRole;
                    }).toList();

                    LOG.info("remove from userMap user that is already assigned to a clientOrganizationUserWithRole");
                    for (ClientOrganizationUserWithRole clientOrganizationUserWithRole : clientOrganizationUserWithRoleList) {
                        User user = userMap.remove(clientOrganizationUserWithRole.getUser().getId());
                        LOG.info("removed user: {}", user);
                    }
                    LOG.info("userMap.values: {}", userMap.values());

                    model.addAttribute("users", userMap.values());
                    model.addAttribute("usersInClientOrganizationUserRole", userInClientOrganizationRoleList);// objects.getT1());
                    LOG.info("added users and usersInClientOrganizationUserRole in model as attributes");
                })
                .thenReturn(PATH)
                .onErrorResume(throwable -> {
                    LOG.error("error occurred: {}", throwable.getMessage());
                    model.addAttribute("error", "please select Organization for this client first.");
                    return Mono.just(PATH);
                });
    }

    private void allowCreateClient(RestPage<CustomPair<String, String>> page, Model model) {
        if (page.totalElements() >= maxClients) {
            model.addAttribute("showCreateClient", "false");
        }
        else {
            model.addAttribute("showCreateClient", "true");
        }
    }

    private ObjectMapper objectMapper = new ObjectMapper();

    private String getJson(Object object) {
        try {
            String json = objectMapper.writeValueAsString(object);

            return json;
        } catch (Exception e) {
            LOG.error("error occued", e);
            return null;
        }
    }
}