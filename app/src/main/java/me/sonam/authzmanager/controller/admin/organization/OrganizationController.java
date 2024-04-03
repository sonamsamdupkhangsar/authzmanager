package me.sonam.authzmanager.controller.admin.organization;

import me.sonam.authzmanager.clients.OauthClientRoute;
import me.sonam.authzmanager.clients.OrganizationWebClient;
import me.sonam.authzmanager.controller.admin.oauth2.OauthClient;
import me.sonam.authzmanager.user.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/organizations")
public class OrganizationController {
    private static final Logger LOG = LoggerFactory.getLogger(OrganizationController.class);

    private OrganizationWebClient organizationWebClient;

    public OrganizationController(OrganizationWebClient organizationWebClient) {
        this.organizationWebClient = organizationWebClient;
    }

    /**
     * get all organizations created/owned by this user
     * @param model
     * @return
     */
    @GetMapping
    public String getOrganizations(Model model) {
        LOG.info("return createForm");

        LOG.info("principal: {}", SecurityContextHolder.getContext().getAuthentication().getPrincipal());

        organizationWebClient.getMyOrganizations().doOnNext(stringStringMap -> {
            model.addAttribute("organizations", stringStringMap);
        }).subscribe();
        return "/admin/organizations/org";
    }

    @GetMapping("/form")
    public String getCreateForm(Model model) {
        LOG.info("return createForm");

        LOG.info("principal: {}", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        model.addAttribute("organization", new Organization());
        return "admin/organizations/form";
    }

    @PostMapping
    public Mono<Rendering> createClient(@ModelAttribute Organization organization, Model model) {
        final String path = "admin/organizations/form";

        LOG.info("create organization from organization: {}", organization);

        UserId userId = (UserId) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Organization org = new Organization(organization.getId(), organization.getName(), userId.getUserId());

       return organizationWebClient.createOrganization(org).flatMap(id -> {
                    LOG.info("got back response: {}", id);
                    model.addAttribute("id", id);
                    Organization organizationUpdated = new Organization(id, organization.getName(), organization.getCreatorUserId());
                    return Mono.just(organizationUpdated);
                }).flatMap(organization1 -> {
            model.addAttribute("organization", organization1);
           return Mono.just(Rendering.view(path).modelAttribute("organization", organization).build());
        });
    }
}
