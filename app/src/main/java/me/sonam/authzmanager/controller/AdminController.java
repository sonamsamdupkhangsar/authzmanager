package me.sonam.authzmanager.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.sonam.authzmanager.advice.SubdomainMenuAdvice;
import me.sonam.authzmanager.controller.util.Util;
import me.sonam.authzmanager.tenant.TenantAuthorizationUrlResolver;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.OrganizationWebClient;
import me.sonam.authzmanager.webclients.RoleWebClient;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private static final Logger LOG = LoggerFactory.getLogger(AdminController.class);
    private static final Duration ROLE_CHECK_TIMEOUT = Duration.ofSeconds(2);

    private final OrganizationWebClient organizationWebClient;
    private final RoleWebClient roleWebClient;
    private final TokenService tokenService;
    private final TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver;

    public AdminController(OrganizationWebClient organizationWebClient,
                           RoleWebClient roleWebClient,
                           TokenService tokenService,
                           TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver) {
        this.organizationWebClient = organizationWebClient;
        this.roleWebClient = roleWebClient;
        this.tokenService = tokenService;
        this.tenantAuthorizationUrlResolver = tenantAuthorizationUrlResolver;
    }

    @GetMapping("/dashboard")
    public String getDashboard(Model model, HttpServletRequest request) {
        LOG.info("return dashboard page");

        LOG.info("principal: {}", SecurityContextHolder.getContext().getAuthentication().getPrincipal());

        model.addAttribute("name", "hello");
        setSubdomainMenuFlag(model, request);
        return "admin/dashboard";
    }

    @PostMapping("/dashboard")
    public String getDashboardPost(Model model, HttpServletRequest request) {
        LOG.info("return dashboard page");

        model.addAttribute("name", "hello");
        setSubdomainMenuFlag(model, request);
        return "admin/dashboard";
    }

    private void setSubdomainMenuFlag(Model model, HttpServletRequest request) {
        Object cached = request.getSession().getAttribute(SubdomainMenuAdvice.SHOW_SUBDOMAIN_MENU_SESSION_ATTRIBUTE);
        if (cached instanceof Boolean showSubdomainMenu) {
            model.addAttribute(SubdomainMenuAdvice.SHOW_SUBDOMAIN_MENU_SESSION_ATTRIBUTE, showSubdomainMenu);
            return;
        }

        String accessToken = tokenService.getAccessToken();
        UUID userId = Util.getLoggedInUserId();
        String host = tenantAuthorizationUrlResolver.currentAuthorizationHost();

        boolean showSubdomainMenu = organizationWebClient.getSubdomainByHost(accessToken, host)
                .flatMap(subdomain -> roleWebClient.isSubdomainAdminInSubdomainId(accessToken, userId, subdomain.getId()))
                .timeout(ROLE_CHECK_TIMEOUT)
                .onErrorResume(throwable -> {
                    LOG.debug("hide Subdomain menu because dashboard SubdomainAdmin check failed", throwable);
                    return Mono.just(false);
                })
                .blockOptional()
                .orElse(false);

        request.getSession().setAttribute(SubdomainMenuAdvice.SHOW_SUBDOMAIN_MENU_SESSION_ATTRIBUTE, showSubdomainMenu);
        model.addAttribute(SubdomainMenuAdvice.SHOW_SUBDOMAIN_MENU_SESSION_ATTRIBUTE, showSubdomainMenu);
    }

    //@GetMapping("/clients")
    public String getClients() {
        LOG.info("return clients");

        SecurityContextHolder.getContext().getAuthentication().getName();

        //oauthClientRoute.getUserClientIds()
        return "admin/clients";
    }


}
