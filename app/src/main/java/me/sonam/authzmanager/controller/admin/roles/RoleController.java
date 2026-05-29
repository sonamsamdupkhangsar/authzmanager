package me.sonam.authzmanager.controller.admin.roles;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import me.sonam.authzmanager.controller.util.MessageConstants;
import me.sonam.authzmanager.rest.RestPage;
import me.sonam.authzmanager.tenant.TenantAuthorizationUrlResolver;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.OrganizationWebClient;
import me.sonam.authzmanager.webclients.RoleWebClient;
import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    private final TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver;

    @Value("${maxRoles}")
    private int maxRoles;

    public RoleController(RoleWebClient roleWebClient, OrganizationWebClient organizationWebClient,
                          TokenService tokenService,
                          TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver) {
        this.roleWebClient = roleWebClient;
        this.organizationWebClient = organizationWebClient;
        this.tokenService = tokenService;
        this.tenantAuthorizationUrlResolver = tenantAuthorizationUrlResolver;
    }

    @GetMapping
    public Mono<String> getRolesByOrganizationId(Model model, Pageable pageable, HttpServletRequest request) {
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

        final String accessToken = tokenService.getAccessToken();
        String organizationHost = tenantAuthorizationUrlResolver.currentAuthorizationHost();

        Pageable finalPageable = pageable;

        return organizationWebClient.getDefaultOrganizationIdForUser(accessToken, userId, organizationHost)
                .flatMap(orgId -> roleWebClient.isSuperAdminInOrgId(accessToken, userId, orgId).zipWith(Mono.just(orgId)))
                .flatMap(objects -> {
                    if (!objects.getT1()) {
                        LOG.error(MessageConstants.NOT_SUPERADMIN);
                        model.addAttribute("error", MessageConstants.NOT_SUPERADMIN);
                    }
                    return roleWebClient.getRolesByOrganizationId(accessToken, objects.getT2(), finalPageable);
                })
                .doOnNext(roleRestPage -> {
                    LOG.info("roleRestPage: {}", roleRestPage.size());
                    model.addAttribute("page", roleRestPage);

                    allowCreateRole(roleRestPage, model);
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
    public Mono<String> updateRole(@Valid  @ModelAttribute("role") Role role, BindingResult bindingResult,
                                   Model model, Pageable userPageable, HttpServletRequest request) {
        final String PATH = "admin/roles/form";
        HttpMethod httpMethod;
        int pageSize = 5;

        if (userPageable.getPageSize() < 100) {
            pageSize = userPageable.getPageSize();
            LOG.info("taking page size from pageable: {}", pageSize);
        }

        Pageable pageable  = PageRequest.of(userPageable.getPageNumber(), pageSize, Sort.by("name"));

        if (role.getId() == null) {
            httpMethod = HttpMethod.POST;
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

        final String accessToken = tokenService.getAccessToken();
        String organizationHost = tenantAuthorizationUrlResolver.currentAuthorizationHost();


        return organizationWebClient.getDefaultOrganizationIdForUser(accessToken, userId, organizationHost)
                .flatMap(defaultOrgId -> {
                    LOG.info("orgId: {}, role.orgId: {}", defaultOrgId, role.getOrganizationId());
                    if (role.getOrganizationId() == null) {
                        role.setOrganizationId(defaultOrgId);//set to defaultOrg
                    }
                    else if (!role.getOrganizationId().equals(defaultOrgId)) {
                        LOG.error("Role is not for the default org");
                        model.addAttribute("error", MessageConstants.ROLE_INVALID_ORGID);
                        return Mono.error(new BadRequestException("Role organization does not match defaultOrgId"));
                    }//else don't do anything

                    LOG.info("return orgId");
                    return Mono.just(defaultOrgId);
                })
               .flatMap(orgId -> roleWebClient.isSuperAdminInOrgId(accessToken, userId, orgId).zipWith(Mono.just(orgId)))
                .flatMap(objects -> {
                   if (!objects.getT1()) {
                       LOG.error(MessageConstants.NOT_SUPERADMIN);
                       model.addAttribute("error", MessageConstants.NOT_SUPERADMIN);
                   }
                   var tempRole = new Role(role.getId(), role.getName(), objects.getT2());
                   return roleWebClient.updateRole(accessToken, tempRole, httpMethod);
               }).flatMap(updateRole -> {
                    LOG.info("got back response: {}", updateRole);
                    model.addAttribute("role", updateRole);
                    model.addAttribute("message", "role updated");
                    return Mono.just(PATH);
                })
              .onErrorResume(throwable -> {
                   model.addAttribute("role", role);
                   model.addAttribute("error", "failed to update/create role: "+throwable.getMessage());
                   return Mono.just(PATH);
               });
    }

    @GetMapping("/{id}")
    public Mono<String> getRoleById(@PathVariable("id") UUID id, Model model, Pageable userPageable,
                                    HttpServletRequest request) {
        final String PATH = "admin/roles/form";
        LOG.info("get role by id: {}", id);
        int pageSize = 5;

        if (userPageable.getPageSize() < 100) {
            pageSize = userPageable.getPageSize();
            LOG.info("taking page size from pageable: {}", pageSize);
        }

        DefaultOidcUser oidcUser = (DefaultOidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID userId = UUID.fromString(oidcUser.getAttribute("userId"));
        LOG.info("userId: {}", userId);

        final String accessToken = tokenService.getAccessToken();
        String organizationHost = tenantAuthorizationUrlResolver.currentAuthorizationHost();

        return  organizationWebClient.getDefaultOrganizationIdForUser(accessToken, userId, organizationHost)
                .flatMap(orgId -> roleWebClient.isSuperAdminInOrgId(accessToken, userId, orgId).zipWith(Mono.just(orgId)))
                .flatMap(objects -> {
                    if (!objects.getT1()) {
                        LOG.error(MessageConstants.NOT_SUPERADMIN);
                        model.addAttribute("error", MessageConstants.NOT_SUPERADMIN);
                    }
                    return roleWebClient.getRoleById(accessToken, id);
                })
                .doOnNext(role -> {
                    model.addAttribute("role", role);
                    LOG.info("role: {}", role);
                })
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
        final String accessToken = tokenService.getAccessToken();

        return roleWebClient.deleteRole(accessToken, roleId).doOnNext(s -> {
                    model.addAttribute("message", "deleted role");
                })
                .then(Mono.just(PATH))
                .onErrorResume(throwable -> {
                    LOG.error("failed to delete role", throwable);
                    model.addAttribute("error", "failed to delete role");
                    return Mono.just(PATH);
        });
    }

    private void allowCreateRole(RestPage<Role> page, Model model) {
        if (page.totalElements() >= maxRoles) {
            model.addAttribute("showCreateRole", "false");
        }
        else {
            model.addAttribute("showCreateRole", "true");
        }
    }


}
