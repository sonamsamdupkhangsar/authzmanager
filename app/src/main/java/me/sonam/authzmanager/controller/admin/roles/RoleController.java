package me.sonam.authzmanager.controller.admin.roles;

import jakarta.validation.Valid;
import me.sonam.authzmanager.oauth2.ConfigurationSettingNames;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.OrganizationWebClient;
import me.sonam.authzmanager.webclients.RoleWebClient;
import me.sonam.authzmanager.user.UserId;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Controller
@RequestMapping("/admin/roles")
public class RoleController {
    private static final Logger LOG = LoggerFactory.getLogger(RoleController.class);

    private RoleWebClient roleWebClient;
    private OrganizationWebClient organizationWebClient;
    private TokenService tokenService;

    public RoleController(RoleWebClient roleWebClient, OrganizationWebClient organizationWebClient, TokenService tokenService) {
        this.roleWebClient = roleWebClient;
        this.organizationWebClient = organizationWebClient;
        this.tokenService = tokenService;
    }

    @GetMapping
    public Mono<String> getRolesByUserId(Model model, Pageable pageable) {
        final String PATH = "admin/roles/list";
        LOG.info("get roles by owner id");
        int pageSize = 5;

        if (pageable.getPageSize() < 100) {
            pageSize = pageable.getPageSize();
            LOG.info("taking page size from pageable: {}", pageSize);
        }

        pageable = PageRequest.of(pageable.getPageNumber(), pageSize, Sort.by("name"));

        LOG.info("pageable: {}", pageable);
        DefaultOidcUser oidcUser = (DefaultOidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userIdAttribute = oidcUser.getAttribute("userId");
        LOG.info("oidc.userId: {}", userIdAttribute);
        UUID userId = UUID.fromString(userIdAttribute);

        return roleWebClient.getRolesByUserId(getAccessToken(), userId, pageable).doOnNext(restPage -> {
            LOG.info("roleList: {}", restPage.getSize());
            model.addAttribute("page", restPage);
        }).then(Mono.just(PATH));
    }

    @GetMapping("/new")
    public Mono<String> getCreateForm(Model model) {
        LOG.info("return createForm");
        final String PATH = "admin/roles/form";

        model.addAttribute("role", new Role());

        return Mono.just(PATH);
    }

    @PostMapping
    public Mono<String> updateRole(@Valid  @ModelAttribute("role") Role role, BindingResult bindingResult, Model model, Pageable userPageable) {
        final String PATH = "admin/roles/form";
        HttpMethod httpMethod = HttpMethod.POST;
        int pageSize = 5;

        if (userPageable.getPageSize() < 100) {
            pageSize = userPageable.getPageSize();
            LOG.info("taking page size from pageable: {}", pageSize);
        }

        Pageable pageable  = PageRequest.of(userPageable.getPageNumber(), pageSize, Sort.by("name"));

        if (role.getId() == null) {
            LOG.info("no id, this is for create");
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
        DefaultOidcUser oidcUser = (DefaultOidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userIdAttribute = oidcUser.getAttribute("userId");
        LOG.info("oidc.userId: {}", userIdAttribute);
        UUID userId = UUID.fromString(userIdAttribute);

        Role role2 = new Role(role.getId(), role.getName(), userId, role.getRoleOrganization());

        LOG.info("role : {}", role);

        final String accessToken = getAccessToken();

       return roleWebClient.updateRole(accessToken, role2, httpMethod).doOnNext(updateRole -> {
                    LOG.info("got back response: {}", updateRole);
                    model.addAttribute("role", updateRole);
                    model.addAttribute("message", "role updated");
        })
               .flatMap(role1 ->  organizationWebClient.getOrganizationPageByOwner(accessToken, userId, pageable))
               .flatMap(organizationRestPage -> {
            LOG.info("organizationList: {}", organizationRestPage);
            //model.addAttribute("organizationPage", organizationRestPage);
                   model.addAttribute("page", organizationRestPage);
            return Mono.just(PATH);
        }).onErrorResume(throwable -> {
           model.addAttribute("role", role2);
           model.addAttribute("error", "failed to update/create role");
           return Mono.just(PATH);
       });
    }

    @GetMapping("/{id}")
    public Mono<String> getRoleById(@PathVariable("id") UUID id, Model model, Pageable userPageable) {
        final String PATH = "admin/roles/form";
        LOG.info("get role by id: {}", id);
        int pageSize = 5;

        if (userPageable.getPageSize() < 100) {
            pageSize = userPageable.getPageSize();
            LOG.info("taking page size from pageable: {}", pageSize);
        }

        Pageable pageable = PageRequest.of(userPageable.getPageNumber(), pageSize, Sort.by("name"));
        DefaultOidcUser oidcUser = (DefaultOidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID userId = UUID.fromString(oidcUser.getAttribute("userId"));
        LOG.info("userId: {}", userId);

        final String accessToken = getAccessToken();

        return roleWebClient.getRoleById(accessToken, id)
                .doOnNext(role -> {
                    model.addAttribute("role", role);
                    LOG.info("role: {}", role);
                })
                .flatMap(roles -> organizationWebClient.getOrganizationPageByOwner(accessToken, userId, pageable).doOnNext(restPage -> {
                    LOG.info("organizationList: {}", restPage);

                  //  model.addAttribute("organizationPage", restPage);
                    model.addAttribute("page", restPage);
                }))
                .thenReturn(PATH);
    }

    /**
     * delete method is called by a Javascript Ajax call. After the delete method call, the ajax will display the '/admin/roles/list' page
     * @param roleId
     * @param model
     * @return
     */
    @DeleteMapping("/{id}")
    public Mono<String> delete(@PathVariable("id") UUID roleId, Model model) {
        final String PATH = "admin/dashboard";//display dashboard template as it doesn't require any data in the model
        LOG.info("delete role by id {}", roleId);

        return roleWebClient.deleteRole(getAccessToken(), roleId).doOnNext(s -> {
                    model.addAttribute("message", "deleted role");
                })
                .then(Mono.just(PATH))
                .onErrorResume(throwable -> {
                    LOG.error("failed to delete role", throwable);
                    model.addAttribute("error", "failed to delete role");
                    return Mono.just(PATH);
        });
    }

    private String getAccessToken() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String accessToken = tokenService.getAccessToken(authentication).getTokenValue();

        return accessToken;
    }
}
