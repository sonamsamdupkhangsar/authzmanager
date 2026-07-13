package me.sonam.authzmanager.controller.admin.organization;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import me.sonam.authzmanager.AuthzManagerException;
import me.sonam.authzmanager.controller.admin.roles.Role;
import me.sonam.authzmanager.controller.util.Util;
import me.sonam.authzmanager.service.OrganizationAuthorizationService;
import me.sonam.authzmanager.service.RoleLimitService;
import me.sonam.authzmanager.tenant.TenantAuthorizationUrlResolver;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.OrganizationWebClient;
import me.sonam.authzmanager.webclients.RoleWebClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Controller
@RequestMapping("/admin/organizations/{organizationId}/roles")
public class OrganizationRoleController {
    private static final String FORM = "admin/organizations/role-form";
    private static final String DASHBOARD = "admin/dashboard";

    private final OrganizationWebClient organizationWebClient;
    private final RoleWebClient roleWebClient;
    private final TokenService tokenService;
    private final TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver;
    private final OrganizationAuthorizationService organizationAuthorizationService;
    private final RoleLimitService roleLimitService;

    public OrganizationRoleController(OrganizationWebClient organizationWebClient,
                                      RoleWebClient roleWebClient,
                                      TokenService tokenService,
                                      TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver,
                                      OrganizationAuthorizationService organizationAuthorizationService,
                                      RoleLimitService roleLimitService) {
        this.organizationWebClient = organizationWebClient;
        this.roleWebClient = roleWebClient;
        this.tokenService = tokenService;
        this.tenantAuthorizationUrlResolver = tenantAuthorizationUrlResolver;
        this.organizationAuthorizationService = organizationAuthorizationService;
        this.roleLimitService = roleLimitService;
    }

    @GetMapping("/new")
    public Mono<String> createForm(@PathVariable UUID organizationId, Model model) {
        Role role = new Role();
        role.setOrganizationId(organizationId);
        return requireOrganization(organizationId, model)
                .doOnNext(organization -> model.addAttribute("role", role))
                .thenReturn(FORM)
                .onErrorResume(throwable -> renderError(model, role, throwable));
    }

    @GetMapping("/{roleId}")
    public Mono<String> editForm(@PathVariable UUID organizationId, @PathVariable UUID roleId, Model model) {
        return requireOrganization(organizationId, model)
                .then(requireRoleInOrganization(roleId, organizationId))
                .doOnNext(role -> model.addAttribute("role", role))
                .thenReturn(FORM)
                .onErrorResume(throwable -> renderError(model, new Role(), throwable));
    }

    @PostMapping
    public Mono<String> save(@PathVariable UUID organizationId,
                             @Valid @ModelAttribute("role") Role submittedRole,
                             BindingResult bindingResult, Model model, HttpServletRequest request) {
        submittedRole.setOrganizationId(organizationId);
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Data validation failed");
            return requireOrganization(organizationId, model).thenReturn(FORM);
        }

        int maxRoles = roleLimitService.maxRolesForHost(tenantAuthorizationUrlResolver.authorizationHost(request));

        return requireOrganization(organizationId, model)
                .flatMap(organization -> {
                    if (submittedRole.getId() == null) {
                        String accessToken = tokenService.getAccessToken();
                        return enforceRoleLimit(accessToken, organizationId, maxRoles)
                                .then(Mono.defer(() -> roleWebClient.updateRole(accessToken,
                                        new Role(null, submittedRole.getName(), organizationId), HttpMethod.POST)));
                    }
                    return requireRoleInOrganization(submittedRole.getId(), organizationId)
                            .flatMap(existingRole -> roleWebClient.updateRole(tokenService.getAccessToken(),
                                    new Role(existingRole.getId(), submittedRole.getName(), organizationId),
                                    HttpMethod.PUT));
                })
                .doOnNext(role -> {
                    model.addAttribute("role", role);
                    model.addAttribute("message", "role updated");
                })
                .thenReturn(FORM)
                .onErrorResume(throwable -> renderError(model, submittedRole, throwable));
    }

    @DeleteMapping("/{roleId}")
    public Mono<String> delete(@PathVariable UUID organizationId, @PathVariable UUID roleId, Model model) {
        return requireOrganization(organizationId, model)
                .then(requireRoleInOrganization(roleId, organizationId))
                .flatMap(role -> roleWebClient.deleteRole(tokenService.getAccessToken(), roleId))
                .doOnNext(message -> model.addAttribute("message", "deleted role"))
                .thenReturn(DASHBOARD)
                .onErrorResume(throwable -> {
                    model.addAttribute("error", "failed to delete role: " + throwable.getMessage());
                    return Mono.just(DASHBOARD);
                });
    }

    private Mono<Organization> requireOrganization(UUID organizationId, Model model) {
        String accessToken = tokenService.getAccessToken();
        UUID userId = Util.getLoggedInUserId();
        String organizationHost = tenantAuthorizationUrlResolver.currentAuthorizationHost();
        return organizationWebClient.getOrganizationById(accessToken, organizationId)
                .doOnNext(organization -> model.addAttribute("organization", organization))
                .flatMap(organization -> organizationAuthorizationService.requireOrgAdminOrSubdomainAdmin(
                        accessToken, userId, organizationHost, organization));
    }

    private Mono<Role> requireRoleInOrganization(UUID roleId, UUID organizationId) {
        return roleWebClient.getRoleById(tokenService.getAccessToken(), roleId)
                .filter(role -> organizationId.equals(role.getOrganizationId()))
                .switchIfEmpty(Mono.error(new AuthzManagerException(
                        "Role does not belong to organization " + organizationId)));
    }

    private Mono<Void> enforceRoleLimit(String accessToken, UUID organizationId, int maxRoles) {
        return roleWebClient.getRolesByOrganizationId(accessToken, organizationId, PageRequest.of(0, 1))
                .flatMap(rolePage -> {
                    if (rolePage.totalElements() >= maxRoles) {
                        return Mono.error(new AuthzManagerException("Max number of roles reached"));
                    }
                    return Mono.empty();
                });
    }

    private Mono<String> renderError(Model model, Role role, Throwable throwable) {
        model.addAttribute("role", role);
        model.addAttribute("error", throwable.getMessage());
        return Mono.just(FORM);
    }
}
