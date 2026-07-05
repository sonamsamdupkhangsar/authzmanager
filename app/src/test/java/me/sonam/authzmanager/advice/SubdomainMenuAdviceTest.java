package me.sonam.authzmanager.advice;

import me.sonam.authzmanager.controller.admin.subdomain.Subdomain;
import me.sonam.authzmanager.tenant.TenantAuthorizationUrlResolver;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.OrganizationWebClient;
import me.sonam.authzmanager.webclients.RoleWebClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubdomainMenuAdviceTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String HOST = "free.openissuer.test";

    @Mock
    private OrganizationWebClient organizationWebClient;
    @Mock
    private RoleWebClient roleWebClient;
    @Mock
    private TokenService tokenService;
    @Mock
    private TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver;

    private SubdomainMenuAdvice advice;
    private Subdomain subdomain;

    @BeforeEach
    void setUp() {
        advice = new SubdomainMenuAdvice(organizationWebClient, roleWebClient, tokenService,
                tenantAuthorizationUrlResolver);
        subdomain = new Subdomain();
        subdomain.setId(UUID.randomUUID());
        subdomain.setHost(HOST);
        setLoggedInUser(USER_ID);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void showsAndCachesMenuForSubdomainAdminByDefault() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/organizations");
        when(tokenService.getAccessToken()).thenReturn("token");
        when(tenantAuthorizationUrlResolver.currentAuthorizationHost()).thenReturn(HOST);
        when(organizationWebClient.getSubdomainByHost("token", HOST)).thenReturn(Mono.just(subdomain));
        when(roleWebClient.isSubdomainAdminInSubdomainId("token", USER_ID, subdomain.getId()))
                .thenReturn(Mono.just(true));

        assertThat(advice.showSubdomainMenu(request)).isTrue();
        assertThat(advice.showSubdomainMenu(request)).isTrue();

        verify(organizationWebClient, times(1)).getSubdomainByHost("token", HOST);
        verify(roleWebClient, times(1)).isSubdomainAdminInSubdomainId("token", USER_ID, subdomain.getId());
    }

    @Test
    void hidesMenuForUserWithoutSubdomainAdmin() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/organizations");
        when(tokenService.getAccessToken()).thenReturn("token");
        when(tenantAuthorizationUrlResolver.currentAuthorizationHost()).thenReturn(HOST);
        when(organizationWebClient.getSubdomainByHost("token", HOST)).thenReturn(Mono.just(subdomain));
        when(roleWebClient.isSubdomainAdminInSubdomainId("token", USER_ID, subdomain.getId()))
                .thenReturn(Mono.just(false));

        assertThat(advice.showSubdomainMenu(request)).isFalse();
    }

    private void setLoggedInUser(UUID userId) {
        OidcIdToken idToken = new OidcIdToken("id-token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("sub", "test-user", "userId", userId.toString()));
        DefaultOidcUser principal = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken);
        SecurityContextHolder.getContext().setAuthentication(new OAuth2AuthenticationToken(principal,
                principal.getAuthorities(), "clientId"));
    }
}
