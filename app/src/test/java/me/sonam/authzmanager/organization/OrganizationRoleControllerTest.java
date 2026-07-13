package me.sonam.authzmanager.organization;

import jakarta.servlet.http.HttpServletRequest;
import me.sonam.authzmanager.controller.admin.organization.Organization;
import me.sonam.authzmanager.controller.admin.organization.OrganizationRoleController;
import me.sonam.authzmanager.controller.admin.roles.Role;
import me.sonam.authzmanager.rest.RestPage;
import me.sonam.authzmanager.service.OrganizationAuthorizationService;
import me.sonam.authzmanager.service.RoleLimitService;
import me.sonam.authzmanager.tenant.TenantAuthorizationUrlResolver;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.OrganizationWebClient;
import me.sonam.authzmanager.webclients.RoleWebClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationRoleControllerTest {
    private static final String ACCESS_TOKEN = "access-token";
    private static final String HOST = "free.openissuer.test";
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private OrganizationWebClient organizationWebClient;
    @Mock
    private RoleWebClient roleWebClient;
    @Mock
    private TokenService tokenService;
    @Mock
    private TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver;
    @Mock
    private OrganizationAuthorizationService organizationAuthorizationService;
    @Mock
    private RoleLimitService roleLimitService;
    @Mock
    private HttpServletRequest request;

    private OrganizationRoleController controller;

    @BeforeEach
    void setUp() {
        controller = new OrganizationRoleController(organizationWebClient, roleWebClient, tokenService,
                tenantAuthorizationUrlResolver, organizationAuthorizationService, roleLimitService);
        when(tokenService.getAccessToken()).thenReturn(ACCESS_TOKEN);
        when(tenantAuthorizationUrlResolver.currentAuthorizationHost()).thenReturn(HOST);
        lenient().when(tenantAuthorizationUrlResolver.authorizationHost(request)).thenReturn(HOST);
        lenient().when(roleLimitService.maxRolesForHost(HOST)).thenReturn(5);
        setLoggedInUser(USER_ID);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsRoleForAuthorizedRouteOrganizationAndIgnoresSubmittedOrganization() {
        UUID organizationId = UUID.randomUUID();
        Organization organization = authorizedOrganization(organizationId);
        Role submitted = new Role(null, "Support", UUID.randomUUID());
        Role saved = new Role(UUID.randomUUID(), "Support", organizationId);
        ExtendedModelMap model = new ExtendedModelMap();
        when(roleWebClient.getRolesByOrganizationId(eq(ACCESS_TOKEN), eq(organizationId), any()))
                .thenReturn(Mono.just(new RestPage<>(List.of(), 0, 1, 0)));
        when(roleWebClient.updateRole(eq(ACCESS_TOKEN), any(Role.class), eq(HttpMethod.POST)))
                .thenReturn(Mono.just(saved));

        assertThat(controller.save(organizationId, submitted,
                new BeanPropertyBindingResult(submitted, "role"), model, request).block())
                .isEqualTo("admin/organizations/role-form");

        ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);
        verify(roleWebClient).updateRole(eq(ACCESS_TOKEN), roleCaptor.capture(), eq(HttpMethod.POST));
        assertThat(roleCaptor.getValue().getOrganizationId()).isEqualTo(organizationId);
        assertThat(model.get("message")).isEqualTo("role updated");
        assertThat(model.get("organization")).isEqualTo(organization);
    }

    @Test
    void updatesRoleOnlyWhenItBelongsToRouteOrganization() {
        UUID organizationId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        authorizedOrganization(organizationId);
        Role submitted = new Role(roleId, "Updated role", UUID.randomUUID());
        Role existing = new Role(roleId, "Existing role", organizationId);
        Role saved = new Role(roleId, "Updated role", organizationId);
        when(roleWebClient.getRoleById(ACCESS_TOKEN, roleId)).thenReturn(Mono.just(existing));
        when(roleWebClient.updateRole(eq(ACCESS_TOKEN), any(Role.class), eq(HttpMethod.PUT)))
                .thenReturn(Mono.just(saved));

        controller.save(organizationId, submitted,
                new BeanPropertyBindingResult(submitted, "role"), new ExtendedModelMap(), request).block();

        ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);
        verify(roleWebClient).updateRole(eq(ACCESS_TOKEN), roleCaptor.capture(), eq(HttpMethod.PUT));
        assertThat(roleCaptor.getValue().getOrganizationId()).isEqualTo(organizationId);
    }

    @Test
    void rejectsRoleThatBelongsToAnotherOrganization() {
        UUID organizationId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        authorizedOrganization(organizationId);
        Role submitted = new Role(roleId, "Updated role", organizationId);
        when(roleWebClient.getRoleById(ACCESS_TOKEN, roleId))
                .thenReturn(Mono.just(new Role(roleId, "Other role", UUID.randomUUID())));
        ExtendedModelMap model = new ExtendedModelMap();

        controller.save(organizationId, submitted,
                new BeanPropertyBindingResult(submitted, "role"), model, request).block();

        assertThat(model.get("error").toString()).contains("Role does not belong to organization");
        verify(roleWebClient, never()).updateRole(eq(ACCESS_TOKEN), any(), any(HttpMethod.class));
    }

    @Test
    void deletesRoleOnlyAfterOrganizationAndRoleOwnershipChecks() {
        UUID organizationId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        authorizedOrganization(organizationId);
        when(roleWebClient.getRoleById(ACCESS_TOKEN, roleId))
                .thenReturn(Mono.just(new Role(roleId, "Support", organizationId)));
        when(roleWebClient.deleteRole(ACCESS_TOKEN, roleId)).thenReturn(Mono.just("deleted"));

        controller.delete(organizationId, roleId, new ExtendedModelMap()).block();

        verify(roleWebClient).deleteRole(ACCESS_TOKEN, roleId);
    }

    @Test
    void deniedOrganizationCannotCreateRole() {
        UUID organizationId = UUID.randomUUID();
        Organization organization = new Organization(organizationId, "Denied", UUID.randomUUID());
        when(organizationWebClient.getOrganizationById(ACCESS_TOKEN, organizationId))
                .thenReturn(Mono.just(organization));
        when(organizationAuthorizationService.requireOrgAdminOrSubdomainAdmin(
                ACCESS_TOKEN, USER_ID, HOST, organization))
                .thenReturn(Mono.error(new IllegalStateException("organization does not belong to subdomain")));
        Role submitted = new Role(null, "Support", organizationId);
        ExtendedModelMap model = new ExtendedModelMap();

        controller.save(organizationId, submitted,
                new BeanPropertyBindingResult(submitted, "role"), model, request).block();

        assertThat(model.get("error").toString()).contains("organization does not belong to subdomain");
        verify(roleWebClient, never()).updateRole(eq(ACCESS_TOKEN), any(), any(HttpMethod.class));
    }

    @Test
    void createRoleStopsBeforeSaveWhenRoleLimitReached() {
        UUID organizationId = UUID.randomUUID();
        authorizedOrganization(organizationId);
        Role submitted = new Role(null, "Support", organizationId);
        ExtendedModelMap model = new ExtendedModelMap();
        when(roleWebClient.getRolesByOrganizationId(eq(ACCESS_TOKEN), eq(organizationId), any()))
                .thenReturn(Mono.just(new RestPage<>(List.of(), 1, 1, 5)));

        controller.save(organizationId, submitted,
                new BeanPropertyBindingResult(submitted, "role"), model, request).block();

        assertThat(model.get("error").toString()).contains("Max number of roles reached");
        verify(roleWebClient, never()).updateRole(eq(ACCESS_TOKEN), any(), any(HttpMethod.class));
    }

    private Organization authorizedOrganization(UUID organizationId) {
        Organization organization = new Organization(organizationId, "Organization", UUID.randomUUID());
        when(organizationWebClient.getOrganizationById(ACCESS_TOKEN, organizationId))
                .thenReturn(Mono.just(organization));
        when(organizationAuthorizationService.requireOrgAdminOrSubdomainAdmin(
                ACCESS_TOKEN, USER_ID, HOST, organization)).thenReturn(Mono.just(organization));
        return organization;
    }

    private void setLoggedInUser(UUID userId) {
        OidcIdToken idToken = new OidcIdToken("id-token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("sub", "test-user", "userId", userId.toString()));
        DefaultOidcUser principal = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken);
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(principal,
                principal.getAuthorities(), "clientId");
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
