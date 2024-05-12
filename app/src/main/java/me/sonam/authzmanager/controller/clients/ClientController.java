package me.sonam.authzmanager.controller.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import me.sonam.authzmanager.webclients.OauthClientWebClient;
import me.sonam.authzmanager.webclients.ClientOrganizationWebClient;
import me.sonam.authzmanager.webclients.OrganizationWebClient;
import me.sonam.authzmanager.webclients.RoleWebClient;
import me.sonam.authzmanager.clients.user.ClientOrganization;
import me.sonam.authzmanager.controller.clients.carrier.ClientOrganizationUserWithRole;
import me.sonam.authzmanager.clients.user.User;
import me.sonam.authzmanager.webclients.UserWebClient;
import me.sonam.authzmanager.oauth2.AuthorizationGrantType;
import me.sonam.authzmanager.oauth2.OauthClient;
import me.sonam.authzmanager.oauth2.RegisteredClient;
import me.sonam.authzmanager.oauth2.util.RegisteredClientUtil;
import me.sonam.authzmanager.controller.admin.organization.Organization;
import me.sonam.authzmanager.user.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
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

    public ClientController(OauthClientWebClient oauthClientWebClient,
                            OrganizationWebClient organizationWebClient,
                            ClientOrganizationWebClient clientOrganizationWebClient,
                            UserWebClient userWebClient, RoleWebClient roleWebClient) {
        this.oauthClientWebClient = oauthClientWebClient;
        this.organizationWebClient = organizationWebClient;
        this.clientOrganizationWebClient = clientOrganizationWebClient;
        this.userWebClient = userWebClient;
        this.roleWebClient = roleWebClient;
    }

    @GetMapping("/createForm")
    public String getCreateForm(Model model) {
        LOG.info("return createForm");
        final String PATH = "admin/clients/form";

        UserId userId = (UserId) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        LOG.info("userId: {}", userId.getUserId());

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

        UserId userId = (UserId) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        LOG.info("userId: {}", userId.getUserId());
        return setClientInModel(id, model, PATH).thenReturn(PATH);
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

    @GetMapping("/hello")
    public Mono<Rendering> hello() {
        final String PATH = "yoman";
        LOG.info("returning yo man");
        return Mono.just(Rendering.view("yoman.html").modelAttribute("client", "I am a client").build());
    }
    /**
     * This will handle the client creation and client update.
     *
     * @param client
     * @param bindingResult
     * @param model
     * @return
     */
    //I removed ModelAttribute("client") because test didn't work
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<String> updateClient(@RequestBody @Valid @ModelAttribute OauthClient client, BindingResult bindingResult, Model model) {

        LOG.info("update client");
        final String PATH = "admin/clients/form";

        LOG.info("client: {}", client);
        if (bindingResult.hasErrors()) {
            LOG.info("client.getId: {}", client.getId());
            LOG.info("user didn't enter required fields: {}, allerror: {}", bindingResult.getFieldError(), bindingResult.getAllErrors());
            model.addAttribute("error", "Data validation failed");
            return Mono.just(PATH);
        }

        UserId userId = (UserId) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        LOG.info("userId: {}", userId.getUserId());

        LOG.info("get map from client");

        RegisteredClient registeredClient = null;
        LOG.info("client.authgranttrypes: {}", client.getAuthorizationGrantTypes());

        if (client.getAuthorizationGrantTypes().contains(AuthorizationGrantType.AUTHORIZATION_CODE.getValue().toUpperCase())) {
            LOG.info("authorizationGrantTypes contains Authorization_Code");
            if (client.getRedirectUris().trim().isEmpty()) {
                final String error = "redirect uris is needed for AuthorizationGrantType of AUTHORIZATION_CODE";
                LOG.error(error);
                bindingResult.rejectValue("redirectUris", "error.user", error);
                return Mono.just(PATH);
            } else {
                LOG.info("redirectUris is not empty: '{}'", client.getRedirectUris());
            }
        }
        HttpMethod httpMethod = HttpMethod.POST;

        try {
            if (client.getId() == null || client.getId().isEmpty()) {
                client.setTokenSettings(null);
                client.setClientSettings(null);
                LOG.info("it's a create client");
            } else {
                httpMethod = HttpMethod.PUT;
                LOG.info("client.id is not null and not empty, it's an update");
            }

            registeredClient = client.getRegisteredClient();
        } catch (Exception e) {
            LOG.error("exception occured when creating client: {}", e.getMessage());

            if (e.getMessage().startsWith("authorizationCodeTimeToLive")) {
                bindingResult.rejectValue("tokenSettings.authorizationCodeTimeToLive", "error.user", "authorizationCodeTimeToLive value must be greater than 0");
            } else if (e.getMessage().startsWith("accessTokenTimeToLive")) {
                bindingResult.rejectValue("tokenSettings.accessTokenTimeToLive", "error.user", "accessTokenTimeToLive value must be greater than 0");
            } else if (e.getMessage().startsWith("deviceCodeTimeToLive")) {
                bindingResult.rejectValue("tokenSettings.deviceCodeTimeToLive", "error.user", "deviceCodeTimeToLive value must be greater than 0");
            } else if (e.getMessage().startsWith("refreshTokenTimeToLive")) {
                bindingResult.rejectValue("tokenSettings.refreshTokenTimeToLive", "error.user", "refreshTokenTimeToLive value must be greater than 0");
            } else {
                LOG.error("unknown error: {}", e.getMessage());
                bindingResult.rejectValue("error", e.getMessage());
            }
            return Mono.just(PATH);
        }

        Map<String, Object> map = registeredClientUtil.getMapObject(registeredClient);
        map.put("mediateToken", client.isMediateToken());

        map.put("userId", userId.getUserId().toString());
        LOG.info("map is {}", map);

        return oauthClientWebClient.updateClient(map, httpMethod).flatMap(updatedRegisteredClient -> {
            LOG.info("client updated and registeredClient returned");

            OauthClient oauthClient = OauthClient.getFromRegisteredClient(updatedRegisteredClient);
            LOG.info("oauthClient {}", oauthClient);

            model.addAttribute("client", oauthClient);
            LOG.info("returning to path: {}", PATH);

            return Mono.just(PATH);
        }).onErrorResume(throwable -> {
            LOG.error("Failed to update client {}", throwable.getMessage());
            model.addAttribute("error", "Failed");

            return Mono.just(PATH);
        });
    }

    @GetMapping
    public Mono<String> getLoggedInUserClients(Model model) {
        LOG.info("get this logged-in users clients only");

        final String PATH = "/admin/clients/list";
        UserId userId = (UserId) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        LOG.info("userId: {}", userId.getUserId());

        return oauthClientWebClient.getUserClientIds(userId.getUserId()).flatMap(pairs -> {
            LOG.info("got clientIds for this userId: {}", userId.getUserId());
            model.addAttribute("clientPairs", pairs);
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

        UserId userId = (UserId) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        LOG.info("userId: {}", userId.getUserId());

        return oauthClientWebClient.deleteClient(id, userId.getUserId())
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
    public Mono<String> getOrganizations(@PathVariable("id") UUID id, Model model, Pageable userPageable) {
        LOG.info("get organizations created or owned by this user-id: {}", id);
        final String PATH = "/admin/clients/organizations";

        Pageable pageable = PageRequest.of(userPageable.getPageNumber(), 5, Sort.by("name"));
        UserId userId = (UserId) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return setClientInModel(id, model, PATH)
                .flatMap(s -> organizationWebClient.getOrganizationPageByOwner(userId.getUserId(), pageable))
                .doOnNext(restPage -> {
                    LOG.info("organizationList: {}", restPage);
                    LOG.info("print organization rest page as json: {}", getJson(restPage));

                    model.addAttribute("page", restPage);
                }).flatMap(organizationRestPage -> {
                    List<ClientOrganization> clientOrganizationList = new ArrayList<>();

                    return clientOrganizationWebClient.getClientIdOrganizationIdMatch(organizationRestPage.getContent(), id)
                    .switchIfEmpty(Mono.just(new ClientOrganization()))
                            .doOnNext(clientOrganization -> {
                                for(Organization organization: organizationRestPage.getContent()) {
                                    if (id.equals(clientOrganization.getClientId()) &&
                                            organization.getId().equals(clientOrganization.getOrganizationId())) {
                                        ClientOrganization clientOrganization1 = new ClientOrganization(id, organization, true);
                                        LOG.info("found a match and add the clientOrganization to list");
                                        clientOrganizationList.add(clientOrganization1);
                                    }
                                    else {
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
     * @param id
     * @param model
     * @param userPageable
     * @return
     */
    @GetMapping("{id}/users")
    public Mono<String> getUsers(@PathVariable("id") UUID id, Model model, Pageable userPageable) {
        return setUsersAndsersInClientOrganizationUserRole(id, model, userPageable);
    }

    @Override
    public Mono<String> setUsersAndsersInClientOrganizationUserRole(UUID id, Model model, Pageable userPageable) {
        LOG.info("get client users relationships");
        final String PATH = "/admin/clients/users";

        Pageable pageable = PageRequest.of(userPageable.getPageNumber(), 5, Sort.by("name"));

        return setClientInModel(id, model, PATH)
                .flatMap(s -> clientOrganizationWebClient.getOrganizationIdAssociatedWithClientId(id))
                .flatMap(uuid -> organizationWebClient.getOrganizationById(uuid))
                .doOnNext(organization -> model.addAttribute("organization", organization))
                .flatMap(organization -> {
                    LOG.info("get roles");
                    return roleWebClient.getRolesByOrganizationId(organization.getId(), PageRequest.of(0, Integer.MAX_VALUE))
                            .zipWith(Mono.just(organization));
                })
                .doOnNext(objects -> {
                    LOG.info("got roles: {}", objects.getT1().getContent());
                    model.addAttribute("roles", objects.getT1().getContent());
                }) //objects = roles, organizationId
                .flatMap(objects -> organizationWebClient.getUsersInOrganizationId(objects.getT2().getId(), pageable).zipWith(Mono.just(objects.getT2())))
                .flatMap(objects -> {
                    LOG.info("uuidPage: {}", objects.getT1().getContent());
                    model.addAttribute("page", objects.getT1());
                    return userWebClient.getUserByBatchOfIds(objects.getT1().getContent()).zipWith(Mono.just(objects.getT2()));
                })
                .doOnNext(objects -> {//objects = users, organization
                    LOG.info("got users: {}", objects.getT1());
                    //  model.addAttribute("users", objects.getT1());
                })
                //find users that have this clientId with a role
                .flatMap(objects -> {
                    List<UUID> userIds = objects.getT1().stream().map(User::getId).toList();
                    return roleWebClient.getClientOrganizationUserWithRoles(id, objects.getT2().getId(), userIds)
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
                    for(ClientOrganizationUserWithRole clientOrganizationUserWithRole: clientOrganizationUserWithRoleList) {
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
                    LOG.error("error occured: {}", throwable.getMessage());
                    model.addAttribute("error", "please select Organization for this client first.");
                    return Mono.just(PATH);
                });
    }

    private ObjectMapper objectMapper = new ObjectMapper();

    private String getJson(Object object) {
        try {
            String json = objectMapper.writeValueAsString(object);

            return json;
        }
        catch (Exception e) {
            LOG.error("error occued", e);
            return null;
        }
    }
}