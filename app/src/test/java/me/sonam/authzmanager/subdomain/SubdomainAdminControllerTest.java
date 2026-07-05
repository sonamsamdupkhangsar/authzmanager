package me.sonam.authzmanager.subdomain;

import me.sonam.authzmanager.clients.role.AuthzManagerRoleAssignment;
import me.sonam.authzmanager.controller.admin.subdomain.Subdomain;
import me.sonam.authzmanager.controller.admin.subdomain.SubdomainAdminController;
import me.sonam.authzmanager.rest.RestPage;
import me.sonam.authzmanager.tenant.TenantAuthorizationUrlResolver;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.OrganizationWebClient;
import me.sonam.authzmanager.webclients.RoleWebClient;
import me.sonam.authzmanager.webclients.UserWebClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.ui.ExtendedModelMap;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubdomainAdminControllerTest {
    private static final String ACCESS_TOKEN = "access-token";
    private static final String HOST = "free.openissuer.test";
    private static final UUID LOGGED_IN_USER_ID = UUID.randomUUID();

    @Mock
    private OrganizationWebClient organizationWebClient;
    @Mock
    private RoleWebClient roleWebClient;
    @Mock
    private UserWebClient userWebClient;
    @Mock
    private TokenService tokenService;
    @Mock
    private TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver;

    private SubdomainAdminController controller;
    private Subdomain subdomain;

    @BeforeEach
    void setUp() {
        controller = new SubdomainAdminController(organizationWebClient, roleWebClient, userWebClient,
                tokenService, tenantAuthorizationUrlResolver);
        subdomain = new Subdomain();
        subdomain.setId(UUID.randomUUID());
        subdomain.setHost(HOST);

        when(tokenService.getAccessToken()).thenReturn(ACCESS_TOKEN);
        when(tenantAuthorizationUrlResolver.currentAuthorizationHost()).thenReturn(HOST);
        setLoggedInUser(LOGGED_IN_USER_ID);
        stubPageRendering();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void assignsEligibleSubdomainAdmin() {
        UUID targetUserId = UUID.randomUUID();
        UUID defaultOrganizationId = UUID.randomUUID();
        AuthzManagerRoleAssignment assignment = new AuthzManagerRoleAssignment(UUID.randomUUID(), UUID.randomUUID(),
                targetUserId, "SUBDOMAIN", subdomain.getId());
        ExtendedModelMap model = new ExtendedModelMap();

        when(organizationWebClient.getDefaultOrganizationIdForUser(ACCESS_TOKEN, targetUserId, HOST))
                .thenReturn(Mono.just(defaultOrganizationId));
        when(roleWebClient.isOrgAdminInOrgId(ACCESS_TOKEN, targetUserId, defaultOrganizationId))
                .thenReturn(Mono.just(true));
        when(roleWebClient.addSubdomainAdmin(ACCESS_TOKEN, subdomain.getId(), targetUserId))
                .thenReturn(Mono.just(assignment));

        assertThat(controller.addSubdomainAdmin(targetUserId, model, PageRequest.of(0, 5)).block())
                .isEqualTo("admin/subdomain/users");
        assertThat(model.get("message")).isEqualTo("SubdomainAdmin assigned");
        verify(roleWebClient).addSubdomainAdmin(ACCESS_TOKEN, subdomain.getId(), targetUserId);
    }

    @Test
    void rejectsUserWithoutDefaultOrganizationInSubdomain() {
        UUID targetUserId = UUID.randomUUID();
        ExtendedModelMap model = new ExtendedModelMap();
        when(organizationWebClient.getDefaultOrganizationIdForUser(ACCESS_TOKEN, targetUserId, HOST))
                .thenReturn(Mono.empty());

        controller.addSubdomainAdmin(targetUserId, model, PageRequest.of(0, 5)).block();

        assertThat(model.get("error").toString())
                .contains("User must have a default organization in this subdomain");
        verify(roleWebClient, never()).addSubdomainAdmin(eq(ACCESS_TOKEN), eq(subdomain.getId()), any());
    }

    @Test
    void rejectsUserWhoIsNotOrgAdminForDefaultOrganization() {
        UUID targetUserId = UUID.randomUUID();
        UUID defaultOrganizationId = UUID.randomUUID();
        ExtendedModelMap model = new ExtendedModelMap();
        when(organizationWebClient.getDefaultOrganizationIdForUser(ACCESS_TOKEN, targetUserId, HOST))
                .thenReturn(Mono.just(defaultOrganizationId));
        when(roleWebClient.isOrgAdminInOrgId(ACCESS_TOKEN, targetUserId, defaultOrganizationId))
                .thenReturn(Mono.just(false));

        controller.addSubdomainAdmin(targetUserId, model, PageRequest.of(0, 5)).block();

        assertThat(model.get("error").toString())
                .contains("User must be OrgAdmin for their default organization");
        verify(roleWebClient, never()).addSubdomainAdmin(eq(ACCESS_TOKEN), eq(subdomain.getId()), any());
    }

    @Test
    void removesSubdomainAdmin() {
        UUID assignmentId = UUID.randomUUID();
        ExtendedModelMap model = new ExtendedModelMap();
        when(roleWebClient.removeSubdomainAdmin(ACCESS_TOKEN, subdomain.getId(), assignmentId))
                .thenReturn(Mono.just("SubdomainAdmin assignment deleted"));

        assertThat(controller.removeSubdomainAdmin(assignmentId, model, PageRequest.of(0, 5)).block())
                .isEqualTo("admin/subdomain/users");
        assertThat(model.get("message")).isEqualTo("SubdomainAdmin removed");
    }

    @Test
    void displaysFinalAdministratorRemovalError() {
        UUID assignmentId = UUID.randomUUID();
        ExtendedModelMap model = new ExtendedModelMap();
        when(roleWebClient.removeSubdomainAdmin(ACCESS_TOKEN, subdomain.getId(), assignmentId))
                .thenReturn(Mono.error(new IllegalStateException("Cannot remove the final SubdomainAdmin")));

        assertThat(controller.removeSubdomainAdmin(assignmentId, model, PageRequest.of(0, 5)).block())
                .isEqualTo("admin/subdomain/users");
        assertThat(model.get("error").toString()).contains("Cannot remove the final SubdomainAdmin");
    }

    private void stubPageRendering() {
        when(organizationWebClient.getSubdomainByHost(ACCESS_TOKEN, HOST)).thenReturn(Mono.just(subdomain));
        when(roleWebClient.isSubdomainAdminInSubdomainId(ACCESS_TOKEN, LOGGED_IN_USER_ID, subdomain.getId()))
                .thenReturn(Mono.just(true));
        when(organizationWebClient.getUsersBySubdomain(eq(ACCESS_TOKEN), eq(HOST), any()))
                .thenReturn(Mono.just(new RestPage<>(List.of(), 0, 5, 0)));
        when(roleWebClient.getSubdomainAdminAssignments(eq(ACCESS_TOKEN), eq(subdomain.getId()), any()))
                .thenReturn(Mono.just(new RestPage<>(List.of(), 0, 1000, 0)));
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
