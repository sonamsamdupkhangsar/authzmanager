package me.sonam.authzmanager.controller.admin;

import jakarta.validation.Valid;
import me.sonam.authzmanager.clients.OauthClientRoute;
import me.sonam.authzmanager.controller.admin.oauth2.AuthorizationGrantType;
import me.sonam.authzmanager.controller.admin.oauth2.OauthClient;
import me.sonam.authzmanager.controller.admin.oauth2.RegisteredClient;
import me.sonam.authzmanager.controller.admin.oauth2.util.RegisteredClientUtil;
import me.sonam.authzmanager.user.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/admin/clients")
public class ClientController {
    private static final Logger LOG = LoggerFactory.getLogger(ClientController.class);

    private OauthClientRoute oauthClientWebClient;
    private RegisteredClientUtil registeredClientUtil = new RegisteredClientUtil();

    public ClientController(OauthClientRoute oauthClientWebClient) {
        this.oauthClientWebClient = oauthClientWebClient;
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


    @GetMapping("/{id}")
    public Mono<String> getClientByClientId(@PathVariable("id") String clientId, Model model) {
        LOG.info("get client by clientId {}", clientId);
        final String PATH = "admin/clients/form";

        UserId userId = (UserId) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        LOG.info("userId: {}", userId.getUserId());
        return oauthClientWebClient.getOauthClientByClientId(clientId).flatMap(registeredClient -> {
            LOG.info("got client {}", registeredClient);
            try {
                OauthClient oauthClient = OauthClient.getFromRegisteredClient(registeredClient);
                LOG.info("oauthClient {}", oauthClient.getClientAuthenticationMethods());
                model.addAttribute("client", oauthClient);

               // parseClientIdUuid(oauthClient);
            }
            catch (Exception e) {
                LOG.error("failed to parse to OautClient", e);
                model.addAttribute("client", new OauthClient());
            }

            LOG.info("return form");
            return Mono.just(PATH);
        }).onErrorResume(throwable -> {
            LOG.error("Failed to get client by clientId", throwable);
            model.addAttribute("client", new OauthClient());
            return Mono.just(PATH);
        });
    }


    /**
     * This will handle the client creation and client update.
     * @param client
     * @param bindingResult
     * @param model
     * @return
     */
    @PostMapping
    public Mono<String> updateClient(@Valid @ModelAttribute("client") OauthClient client, BindingResult bindingResult, Model model) {
        LOG.info("update client");
        final String PATH = "admin/clients/form";

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
            }
            else {
                LOG.info("redirectUris is not empty: '{}'", client.getRedirectUris());
            }
        }
        HttpMethod httpMethod = HttpMethod.POST;

        try {
            if (client.getId() == null || client.getId().isEmpty()) {
                client.setTokenSettings(null);
                client.setClientSettings(null);
                LOG.info("it's a create client");
            }
            else {
                httpMethod = HttpMethod.PUT;
                LOG.info("client.id is not null and not empty, it's an update");
            }

            registeredClient = client.getRegisteredClient();
        }
        catch (Exception e) {
            LOG.error("exception occured when creating client: {}", e.getMessage());

            if (e.getMessage().startsWith("authorizationCodeTimeToLive")) {
                bindingResult.rejectValue("tokenSettings.authorizationCodeTimeToLive",  "error.user", "authorizationCodeTimeToLive value must be greater than 0");
            }
            else if (e.getMessage().startsWith("accessTokenTimeToLive")) {
                bindingResult.rejectValue("tokenSettings.accessTokenTimeToLive", "error.user", "accessTokenTimeToLive value must be greater than 0");
            }
            else if (e.getMessage().startsWith("deviceCodeTimeToLive")) {
                bindingResult.rejectValue("tokenSettings.deviceCodeTimeToLive", "error.user", "deviceCodeTimeToLive value must be greater than 0");
            }
            else if (e.getMessage().startsWith("refreshTokenTimeToLive")) {
                bindingResult.rejectValue("tokenSettings.refreshTokenTimeToLive","error.user", "refreshTokenTimeToLive value must be greater than 0");
            }
            else {
                LOG.error("unknown error: {}", e.getMessage());
                bindingResult.rejectValue("error", e.getMessage());
            }
            return Mono.just(PATH);
        }

        Map<String, Object> map = registeredClientUtil.getMapObject(registeredClient);
        map.put("mediateToken", client.isMediateToken());

        map.put("userId", userId.getUserId().toString());
        LOG.info("map is {}", map);

        return  oauthClientWebClient.updateClient(map, httpMethod).flatMap(updatedRegisteredClient -> {
            LOG.info("client updated and registeredClient returned");

            OauthClient oauthClient = OauthClient.getFromRegisteredClient(updatedRegisteredClient);
            LOG.info("oauthClient {}", oauthClient);
            model.addAttribute("client", oauthClient);
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

        return oauthClientWebClient.getUserClientIds(userId.getUserId()).flatMap(clientIds -> {
            LOG.info("got clientIds for this userId: {}", userId.getUserId());
            model.addAttribute("clientIds", clientIds);
            return Mono.just(PATH);
        }).onErrorResume(throwable -> {
            LOG.error("error occued on calling get user clientIds: {}", throwable.getMessage());
            model.addAttribute("message", "failed");
            return Mono.just(PATH);
        });
    }

    @DeleteMapping("/{clientId}")
    public Mono<String> deleteClientByClientId(@PathVariable("clientId")String clientId, Model model) {
        LOG.info("delete client by clientId: {}", clientId);
        final String PATH = "/admin/clients/list";

        UserId userId = (UserId) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        LOG.info("userId: {}", userId.getUserId());

        return oauthClientWebClient.deleteClient(clientId, userId.getUserId()).thenReturn(PATH)
                .onErrorResume(throwable -> {
                    LOG.error("error occurred on deleting client by clientId: {}", clientId, throwable);
                    model.addAttribute("error", "Failed to delete by clientId");
                    return Mono.just(PATH);
                });

    }
}
