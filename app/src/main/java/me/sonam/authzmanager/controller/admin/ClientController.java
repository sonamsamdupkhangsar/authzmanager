package me.sonam.authzmanager.controller.admin;

import me.sonam.authzmanager.controller.admin.oauth2.OauthClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/clients")
public class ClientController {
    private static final Logger LOG = LoggerFactory.getLogger(ClientController.class);

    @GetMapping("/createForm")
    public String getCreateForm(Model model) {
        LOG.info("return createForm");

        LOG.info("principal: {}", SecurityContextHolder.getContext().getAuthentication().getPrincipal());

        model.addAttribute("client", new OauthClient());
        return "admin/clients/form";
    }

    @PostMapping("/create")
    public String createClient(@ModelAttribute OauthClient client, Model model) {
        LOG.info("return create client");

        LOG.info("principal: {}", SecurityContextHolder.getContext().getAuthentication().getPrincipal());

        LOG.info("oauth client to create is {}", client);

        model.addAttribute("client", client);
        return "admin/clients/form";
    }
}
