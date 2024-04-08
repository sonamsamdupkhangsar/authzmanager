package me.sonam.authzmanager.controller.admin.organization;

import jakarta.validation.Valid;
import jakarta.ws.rs.Path;
import me.sonam.authzmanager.clients.OauthClientRoute;
import me.sonam.authzmanager.clients.OrganizationWebClient;
import me.sonam.authzmanager.controller.admin.oauth2.OauthClient;
import me.sonam.authzmanager.user.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    public Mono<String> getOrganizations(Model model) {
        LOG.info("return createForm");
        final String PATH = "/admin/organizations/list";

        UserId userId = (UserId) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return organizationWebClient.getMyOrganizations(userId.getUserId()).doOnNext(restPage -> {
            LOG.info("organizationList: {}", restPage);
            model.addAttribute("page", restPage);
        }).then(Mono.just(PATH));
    }

    @GetMapping("/new")
    public Mono<String> getCreateForm(Model model) {
        LOG.info("return createForm");
        final String PATH = "admin/organizations/form";
        model.addAttribute("organization", new Organization());

        return Mono.just(PATH);
    }

    @PostMapping
    public Mono<String> updateOrganization(@Valid  @ModelAttribute("organization") Organization organization, BindingResult bindingResult, Model model) {
        final String PATH = "admin/organizations/form";
        HttpMethod httpMethod = HttpMethod.POST;

        if (organization.getId() == null) {
            LOG.info("no id, this is for create");
            httpMethod = HttpMethod.POST;
        }
        else {
            LOG.info("has id, this is for update");
            httpMethod = HttpMethod.PUT;
        }
        if (bindingResult.hasErrors()) {
            LOG.info("user didn't enter required fields");
            model.addAttribute("error", "Data validation failed");
            return Mono.just(PATH);
        }
        UserId userId = (UserId) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Organization org = new Organization(organization.getId(), organization.getName(), userId.getUserId());
        LOG.info("create organization from organization: {}", organization);

       return organizationWebClient.updateOrganization(org, httpMethod).flatMap(organization1 -> {
                    LOG.info("got back response: {}", organization1);
                    model.addAttribute("organization", organization1);
                    return Mono.just(PATH);
        });
    }

    @GetMapping("/{id}")
    public Mono<String> getOrganizationById(@PathVariable("id") UUID id, Model model) {
        final String PATH = "admin/organizations/form";
        LOG.info("get organization by id: {}", id);

        return organizationWebClient.getOrganizationById(id)
                .doOnNext(organization -> model.addAttribute("organization", organization))
                .thenReturn(PATH);
    }

    @DeleteMapping("/{id}")
    public Mono<String> delete(@PathVariable("id") UUID organizationId, Model model) {
        final String PATH = "admin/organizations/organization";
        LOG.info("delete organization by id {}", organizationId);

        return organizationWebClient.deleteOrganization(organizationId).doOnNext(s -> {
                    model.addAttribute("message", "deleted organization");
                })
                .then(Mono.just(PATH))
                .onErrorResume(throwable -> {
                    LOG.error("failed to delete organization", throwable);
                    model.addAttribute("error", "failed to delete organization");
                    return Mono.just(PATH);
        });
    }
}
