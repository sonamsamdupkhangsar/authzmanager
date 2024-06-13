package me.sonam.authzmanager.controller.admin.roles;

import me.sonam.authzmanager.clients.user.ClientOrganization;
import me.sonam.authzmanager.controller.admin.organization.Organization;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.user.UserId;
import me.sonam.authzmanager.webclients.OrganizationWebClient;
import me.sonam.authzmanager.webclients.RoleWebClient;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/roles/{id}/organizations")
public class RoleOrganizationController {
    private static final Logger LOG = LoggerFactory.getLogger(RoleOrganizationController.class);

    private RoleWebClient roleWebClient;
    private OrganizationWebClient organizationWebClient;
    final String PATH = "admin/roles/organizations";
    private TokenService tokenService;

    public RoleOrganizationController(RoleWebClient roleWebClient, OrganizationWebClient organizationWebClient, TokenService tokenService) {
        this.roleWebClient = roleWebClient;
        this.organizationWebClient = organizationWebClient;
        this.tokenService = tokenService;
    }

    @GetMapping
    public Mono<String> getOrganizations(@PathVariable("id") UUID id, Model model, Pageable userPageable) {
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
                .flatMap(role -> organizationWebClient.getOrganizationPageByOwner(accessToken, userId, pageable).zipWith(Mono.just(role))
                .doOnNext(objects -> {
                    LOG.info("organizationList: {}", objects.getT1());

                    //  model.addAttribute("organizationPage", restPage);
                    model.addAttribute("page", objects.getT1());
                }))
                /*.doOnNext(objects -> {
                    Role role = objects.getT2();
                    List<RoleOrganization> roleOrganizationList = new ArrayList<>();

                    for(Organization organization: objects.getT1().getContent()) {
                        if (role.getRoleOrganization() != null &&
                                role.getRoleOrganization().getOrganizationId().equals(organization.getId())) {
                                RoleOrganization roleOrganization =
                                        new RoleOrganization(role.getRoleOrganization().getId(), role.getId(),
                                                role.getRoleOrganization().getOrganizationId(), true);
                                roleOrganizationList.add(roleOrganization);
                                LOG.info("role has a organization associated, adding to list");
                        }
                        else {
                            RoleOrganization roleOrganization =
                                    new RoleOrganization(null, role.getId(),
                                            organization.getId(), false);
                            roleOrganizationList.add(roleOrganization);
                            LOG.info("role has a organization associated, adding to list");
                        }
                    }
                    model.addAttribute("roleOrganization", roleOrganizationList);
                })*/
                .thenReturn(PATH);
    }

    @PostMapping
    public Mono<String> addRoleToOrganization(RoleOrganization roleOrganization, @PathVariable("id") UUID id, Model model , Pageable userPageable) {
        roleOrganization.setId(null);
        LOG.info("set id to null, add role to organization: {}", roleOrganization);

        int pageSize = 5;

        if (userPageable.getPageSize() < 100) {
            pageSize = userPageable.getPageSize();
            LOG.info("taking page size from pageable: {}", pageSize);
        }

        Pageable pageable = PageRequest.of(userPageable.getPageNumber(), pageSize, Sort.by("name"));
        DefaultOidcUser oidcUser = (DefaultOidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userIdAttribute = oidcUser.getAttribute("userId");
        LOG.info("oidc.userId: {}", userIdAttribute);
        UUID userId = UUID.fromString(userIdAttribute);

        final String accessToken = getAccessToken();

        return roleWebClient.addRoleToOrganization(accessToken, roleOrganization)
                .doOnNext(roleOrganization1 -> {
                    LOG.info("added role to organization");
                    model.addAttribute("message", "assigned role to organization successfully");
                }).flatMap(roleOrganization1 -> roleWebClient.getRoleById(accessToken, id))
                .doOnNext(role -> {
                    model.addAttribute("role", role);
                    LOG.info("role: {}", role);
                })
                .flatMap(role -> organizationWebClient.getOrganizationPageByOwner(accessToken, userId, pageable).zipWith(Mono.just(role))
                        .doOnNext(objects -> {
                            LOG.info("organizationList: {}", objects.getT1());
                            model.addAttribute("page", objects.getT1());
                        }))
                .thenReturn(PATH);

    }

    @DeleteMapping(path="{organizationId}")
    public Mono<String> deleteRoleOrganization(@PathVariable("id") UUID roleId, @PathVariable("organizationId") UUID organizationId, Model model, Pageable userPageable) {
        final String DASHBOARD_PATH = "/admin/dashboard";
        LOG.info("delete roleOrganization by roleId '{}' and organizationId: '{}'", roleId, organizationId);
        int pageSize = 5;

        if (userPageable.getPageSize() < 100) {
            pageSize = userPageable.getPageSize();
            LOG.info("taking page size from pageable: {}", pageSize);
        }

        Pageable pageable = PageRequest.of(userPageable.getPageNumber(), pageSize, Sort.by("name"));
        DefaultOidcUser oidcUser = (DefaultOidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userIdAttribute = oidcUser.getAttribute("userId");
        LOG.info("oidc.userId: {}", userIdAttribute);
        UUID userId = UUID.fromString(userIdAttribute);

        final String accessToken = getAccessToken();

        return roleWebClient.deleteRoleOrganization(accessToken, roleId, organizationId)
                .doOnNext(string -> {
                    LOG.info("deleted roleOrganization");
                    model.addAttribute("message", "delete roleOrganization successfully");
                }).flatMap(roleOrganization1 -> roleWebClient.getRoleById(accessToken, organizationId))
                .doOnNext(role -> {
                    model.addAttribute("role", role);
                    LOG.info("role: {}", role);
                })
                .flatMap(role -> organizationWebClient.getOrganizationPageByOwner(accessToken, userId, pageable).zipWith(Mono.just(role))
                        .doOnNext(objects -> {
                            LOG.info("organizationList: {}", objects.getT1());
                            model.addAttribute("page", objects.getT1());
                        }))
                .thenReturn(DASHBOARD_PATH);

    }

    private String getAccessToken() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String accessToken = tokenService.getAccessToken(authentication).getTokenValue();

        return accessToken;
    }
}
