package me.sonam.authzmanager.controller;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private static final Logger LOG = LoggerFactory.getLogger(AdminController.class);

    @GetMapping("/dashboard")
    public String getDashboard(Model model) {
        LOG.info("return dashboard page");

        model.addAttribute("name", "hello");
        return "admin/dashboard";
    }
}
