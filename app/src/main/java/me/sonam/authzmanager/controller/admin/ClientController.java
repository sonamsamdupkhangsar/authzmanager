package me.sonam.authzmanager.controller.admin;

import jakarta.validation.Valid;
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
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
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

    @PostMapping("/create")
    public Mono<String> createClient(@Valid @ModelAttribute("client") OauthClient client, BindingResult bindingResult, Model model) {
        LOG.info("create client");
        final String PATH = "admin/clients/updateClientForm";

        if (bindingResult.hasErrors()) {
            LOG.info("user didn't enter required fields");
            model.addAttribute("error", "Data validation failed");
            return Mono.just("admin/clients/form");
        }

      /*  if (client.getClientId() == null || client.getClientId().isEmpty() || client.getClientId().length() < 5) {
            bindingResult.addError(new ObjectError("clientId", "Give a client name ( > 5 character length)"));
            LOG.info("clientId error added when empty or less than 5 characters in length");

            model.addAttribute("error", "Data validation failed");
            return Mono.just("admin/clients/form");
        }*/

        UserId userId = (UserId) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        LOG.info("userId: {}", userId.getUserId());


        LOG.info("get map from client");

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
             //parseClientIdUuid(oauthClient);

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

               // parseClientIdUuid(oauthClient);
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

/*    private void parseClientIdUuid(OauthClient oauthClient) {
        LOG.info("parse clientId uuid");

        int indexOf = oauthClient.getClientId().indexOf(".");
        String uuidString = oauthClient.getClientId().substring(0, indexOf);
        if (indexOf > 0) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                oauthClient.setClientIdUuid(uuid);
                LOG.info("it is a uuid: {}", uuid);
                String afterUuidString = oauthClient.getClientId().substring(indexOf+1);
                LOG.info("text after uuid: {}", afterUuidString);
                oauthClient.setClientId(afterUuidString);
            }
            catch (Exception e) {
                LOG.error("is not a uuid", e);
            }
        }
    }*/

    @PostMapping("/update")
    public Mono<String> updateClient(@Valid @ModelAttribute("client") OauthClient client, BindingResult bindingResult, Model model) {
        LOG.info("update client");
        final String PATH = "admin/clients/updateClientForm";


        if (bindingResult.hasErrors()) {
            LOG.info("user didn't enter required fields");
            model.addAttribute("error", "Data validation failed");
            return Mono.just(PATH);
        }

        UserId userId = (UserId) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        LOG.info("userId: {}", userId.getUserId());

        LOG.info("get map from client");

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

                //parseClientIdUuid(oauthClient);

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
