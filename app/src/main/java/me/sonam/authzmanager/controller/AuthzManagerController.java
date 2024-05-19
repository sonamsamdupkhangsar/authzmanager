package me.sonam.authzmanager.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class AuthzManagerController {
    private static final Logger LOG = LoggerFactory.getLogger(AuthzManagerController.class);

    @GetMapping("/greeting")
    public String getIndex(Model model) {
        LOG.info("return index page");

        model.addAttribute("name", "hello");
        return "greeting";
    }

     @GetMapping("/login/login.html")
    public String getLogin(Model model) {
        LOG.info("return login page");

        model.addAttribute("name", "hello");
        return "login/login";
    }

    @GetMapping
    public String getIndex() {
        LOG.info("return index page");
        return "index";
    }



}
