package me.sonam.authzmanager.controller.admin;

import me.sonam.authzmanager.clients.OauthClientRoute;
import me.sonam.authzmanager.controller.admin.oauth2.ConfigurationSettingNames;
import me.sonam.authzmanager.controller.admin.oauth2.OauthClient;
import me.sonam.authzmanager.controller.admin.oauth2.RegisteredClient;
import me.sonam.authzmanager.controller.admin.oauth2.TokenSettings;
import me.sonam.authzmanager.controller.admin.oauth2.util.RegisteredClientUtil;
import me.sonam.authzmanager.user.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

        model.addAttribute("client", new OauthClient());
        return PATH;
    }

    @GetMapping("/{id}")
    public Mono<String> getClientByClientId(@PathVariable("id") String clientId, Model model) {
        LOG.info("get client by clientId {}", clientId);
        final String PATH = "admin/clients/updateClientForm";

        UserId userId = (UserId) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        LOG.info("userId: {}", userId.getUserId());
        return oauthClientWebClient.getOauthClientByClientId(clientId).flatMap(registeredClient -> {
            LOG.info("got client {}", registeredClient);
            try {
                OauthClient oauthClient = OauthClient.getFromRegisteredClient(registeredClient);
                LOG.info("oauthClient {}", oauthClient.getClientAuthenticationMethods());
                model.addAttribute("client", oauthClient);
            }
            catch (Exception e) {
                LOG.error("failed to parse to OautClient", e);
                model.addAttribute("client", new OauthClient());
            }

            LOG.info("return updateClientForm");
            return Mono.just(PATH);
        }).onErrorResume(throwable -> {
            LOG.error("Failed to get client by clientId", throwable);
            model.addAttribute("client", new OauthClient());
            return Mono.just(PATH);
        });
    }

    @PostMapping("/create")
    public Mono<String> createClient(@ModelAttribute OauthClient client, Model model) {
        LOG.info("create client");
        final String PATH = "admin/clients/updateClientForm";

        UserId userId = (UserId) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        LOG.info("userId: {}", userId.getUserId());


        LOG.info("get map from client");
        //Map<String, Object> map = client.getMap();
        //map.put("userId", userId.getUserId().toString());
        //LOG.info("map is {}", map);
        client.setId(UUID.randomUUID().toString());
        // on initial client creation user won't see the token settings or client settings for simplicity
        // so set them to null
        client.setTokenSettings(null);
        client.setClientSettings(null);
        if (client.getRedirectUris() == null) {
            client.setRedirectUris("");
        }
        RegisteredClient registeredClient = client.getRegisteredClient();

        Map<String, Object> map = registeredClientUtil.getMapObject(registeredClient);
        //Map<String, Object> map = client.getMap();
        map.put("userId", userId.getUserId().toString());
        LOG.info("map is {}", map);

        return  oauthClientWebClient.createClient(map).flatMap(registeredClient1 -> {
            LOG.info("get OauthClient from registeredClient1");
            OauthClient oauthClient = OauthClient.getFromRegisteredClient(registeredClient);

              model.addAttribute("client", oauthClient);
              model.addAttribute("message", "Success");
              return Mono.just(PATH);
              //return Mono.just(Rendering.view(PATH).modelAttribute("client", client).build());
          }).onErrorResume(throwable -> {
              LOG.error("Failed to create client {}", throwable.getMessage());
              model.addAttribute("error", "Failed");
              return Mono.just(PATH);
          });
    }

    @PostMapping("/update")
    public Mono<String> updateClient(@ModelAttribute OauthClient client, Model model) {
        LOG.info("update client");
        final String PATH = "admin/clients/updateClientForm";

        UserId userId = (UserId) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        LOG.info("userId: {}", userId.getUserId());

        LOG.info("get map from client");

        if (client.getId() == null || client.getId().isEmpty()) {
            client.setId(UUID.randomUUID().toString());
        }
        RegisteredClient registeredClient = client.getRegisteredClient();
        Map<String, Object> map = registeredClientUtil.getMapObject(registeredClient);
        map.put("mediateToken", client.isMediateToken());

        map.put("userId", userId.getUserId().toString());
        LOG.info("map is {}", map);

        return  oauthClientWebClient.updateClient(map).flatMap(updatedRegisteredClient -> {
            LOG.info("client updated and registeredClient returned");
            try {

                OauthClient oauthClient = OauthClient.getFromRegisteredClient(registeredClient);
                LOG.info("oauthClient.tokenSettings.authorizationCodeTImeToLive: {}", oauthClient.getTokenSettings().getAuthorizationCodeTimeToLive());


                LOG.info("oauthClient {}", oauthClient);
                model.addAttribute("client", oauthClient);
            }
            catch (Exception e) {
                LOG.error("failed to parse to OauthClient", e);
                model.addAttribute("client", new OauthClient());
                model.addAttribute("error", "Failed");
            }
            return Mono.just(PATH);
            //return Mono.just(Rendering.view(PATH).modelAttribute("client", client).build());
        }).onErrorResume(throwable -> {
            LOG.error("Failed to create client {}", throwable.getMessage());
            model.addAttribute("error", "Failed");
            return Mono.just(PATH);
        });
    }

    @GetMapping
    public Mono<String> getLoggedInUserClients(Model model) {
        LOG.info("get this logged-in users clients only");

        final String PATH = "/admin/clients";
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
}
