package me.sonam.authzmanager.advice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class SubdomainMenuAdvice {
    public static final String SHOW_SUBDOMAIN_MENU_SESSION_ATTRIBUTE = "showSubdomainMenu";

    @ModelAttribute("showSubdomainMenu")
    public boolean showSubdomainMenu(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object cached = session.getAttribute(SHOW_SUBDOMAIN_MENU_SESSION_ATTRIBUTE);
            if (cached instanceof Boolean showSubdomainMenu) {
                return showSubdomainMenu;
            }
        }

        return false;
    }
}
