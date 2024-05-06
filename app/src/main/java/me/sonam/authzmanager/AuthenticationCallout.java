package me.sonam.authzmanager;

import me.sonam.authzmanager.tokenfilter.TokenFilter;
import me.sonam.authzmanager.user.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * This authenticationProvider will make a call-out to Spring Authorization Server rest service
 * to authenticate.  This does not require a client id so the returned roles may be empty.
 */
public class AuthenticationCallout  implements AuthenticationProvider {
    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationCallout.class);
    public WebClient.Builder webClientBuilder;
    private WebClient webClient;

    private final String springAuthorizationServerAuthenticationEp;

    private TokenFilter tokenFilter;
    @Value("${oauthClientId}")
    private String oauthClientId;

    public AuthenticationCallout(WebClient.Builder webClientBuilder, final String springAuthorizationServerAuthenticationEp) {
        this.webClientBuilder = webClientBuilder;
        this.springAuthorizationServerAuthenticationEp = springAuthorizationServerAuthenticationEp;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        LOG.info("request to authenticate");
        return callSpringAuthorizationServer(authentication.getPrincipal().toString(),
                authentication.getCredentials().toString()).block();
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }

    private Mono<? extends Authentication> callSpringAuthorizationServer(final String username, final String password) {
        LOG.info("calling Spring Authorization Server for authentication endpoint: {}", springAuthorizationServerAuthenticationEp);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().put().uri(springAuthorizationServerAuthenticationEp)
                .bodyValue(Map.of("username", username, "password", password, "clientId", oauthClientId))
                .headers(httpHeaders -> httpHeaders.setContentType(MediaType.APPLICATION_JSON))
                .retrieve();

        return responseSpec.bodyToMono(Map.class).map(roleMap -> {
            LOG.info("got response from auth-server authenticate call: {}", roleMap);  //returned roles delimited by space from auth-server authenticate endpoint  like "USER ADMIN"
            final List<GrantedAuthority> grantedAuths = new ArrayList<>();

            if (roleMap.get("roles") != null) {
                String[] tempRoles = roleMap.get("roles").toString().split(" ");
                for (String role : tempRoles) {
                    LOG.info("add role: {}", role);
                    if (!role.trim().isEmpty()) {
                        grantedAuths.add(new SimpleGrantedAuthority(role));
                    }
                }
            }
            if (grantedAuths.isEmpty()) {
                LOG.info("add a user role if no role found for managing authzmanager");
                grantedAuths.add(new SimpleGrantedAuthority("USER"));
            }

            UUID userId = UUID.fromString(roleMap.get("userId").toString());

            final UserDetails principal = new UserId(userId, username, password, grantedAuths);

            return new UsernamePasswordAuthenticationToken(principal, password, grantedAuths);
        }).onErrorResume(throwable -> {
            LOG.error("failed to authenticate using Spring Authorization Server authentication: {}", throwable.getMessage());
            return Mono.error(new BadCredentialsException("failed to authenticate with username and password"));
        });
    }

}
