package me.sonam.authzmanager.controller;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @GetMapping
    public String getIndex() {
        LOG.info("return index page");
        return "index";
    }

    @Autowired
    private HttpSession session;
    @GetMapping("/userlogout")
    public String getLogout(HttpServletRequest request) {
        LOG.info("return logout");

        LOG.info("invalidate session");
        session.invalidate();


        return "userlogout";
    }


    @GetMapping("/oauth2-login-error")
    public String loginError() {
        LOG.info("login error occurred");

        session.invalidate();
        return "oauth2-login-error";
    }
}
