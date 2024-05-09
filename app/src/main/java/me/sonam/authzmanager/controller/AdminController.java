package me.sonam.authzmanager.controller;

import me.sonam.authzmanager.clients.OauthClientRoute;
import me.sonam.authzmanager.webclients.OauthClientWebClient;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private static final Logger LOG = LoggerFactory.getLogger(AdminController.class);

    public AdminController(OauthClientWebClient oauthClientWebClient) {
    }

    @GetMapping("/dashboard")
    public String getDashboard(Model model) {
        LOG.info("return dashboard page");

        LOG.info("principal: {}", SecurityContextHolder.getContext().getAuthentication().getPrincipal());

        model.addAttribute("name", "hello");
        return "admin/dashboard";
    }

    @PostMapping("/dashboard")
    public String getDashboardPost(Model model) {
        LOG.info("return dashboard page");

        model.addAttribute("name", "hello");
        return "admin/dashboard";
    }

    //@GetMapping("/clients")
    public String getClients() {
        LOG.info("return clients");

        SecurityContextHolder.getContext().getAuthentication().getName();

        //oauthClientRoute.getUserClientIds()
        return "admin/clients";
    }


}
