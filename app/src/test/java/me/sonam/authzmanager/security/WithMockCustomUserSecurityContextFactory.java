package me.sonam.authzmanager.security;

import me.sonam.authzmanager.user.UserId;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames.ID_TOKEN;

public class WithMockCustomUserSecurityContextFactory implements WithSecurityContextFactory<WithMockCustomUser> {
    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser customUser) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        final List<GrantedAuthority> grantedAuths = new ArrayList<>();
        grantedAuths.add(new SimpleGrantedAuthority(customUser.role()));

        //final UserDetails principal = new UserId(customUser.userId(), customUser.name(), "password", grantedAuths);
        OidcIdToken idToken = new OidcIdToken(ID_TOKEN, Instant.now(),
                Instant.now().plusSeconds(60), Map.of("role", "USER_ROLE", "sub", "sonam", "userId", customUser.userId()));
        DefaultOidcUser principal = new DefaultOidcUser(grantedAuths, idToken);

       // OAuth2AuthenticatedPrincipal auth = new DefaultOAuth2AuthenticatedPrincipal("sonam", Map.of("role", "USER_ROLE"),
        //        AuthorityUtils.createAuthorityList("SCOPE_message:read"));;

          Authentication auth =       //  UsernamePasswordAuthenticationToken.authenticated(principal, "password", principal.getAuthorities());
        new OAuth2AuthenticationToken(principal, principal.getAuthorities(),  "clientId");

        context.setAuthentication(auth);
        return context;
    }
}
