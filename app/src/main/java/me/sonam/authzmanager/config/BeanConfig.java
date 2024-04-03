package me.sonam.authzmanager.config;

import me.sonam.authzmanager.AuthenticationCallout;
import me.sonam.authzmanager.clients.*;
import me.sonam.authzmanager.user.UserRoute;
import me.sonam.authzmanager.user.UserRouteAuthServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class BeanConfig {
    @Value("{user-rest-service.root}${user-rest-service.signup}")
    private String userSignupEndpoint;

    @Value("{authentication-rest-service.root}${authentication-rest-service.authenticate}")
    private String authenticateEndpoint;
    @Value("${auth-server.root}${auth-server.clients}")
    private String authServerClientsEndpoint;
    @Value("${auth-server.root}${auth-server.authenticate}")
    private String springAuthorizationServerAuthenticationEp;

    @Value("${organization-rest-service.root}${organization-rest-service.contextPath}")
    private String organizationEndpoint;

    @Autowired
    @Qualifier("regular")
    private WebClient.Builder webClientBuilder;

    @Autowired
    @Qualifier("webClientWithTokenFilter")
    private WebClient.Builder webClientWithTokenFilter;

    @Bean
    public UserRoute userRoute() {
        return new UserRouteAuthServer(webClientWithTokenFilter, userSignupEndpoint, authenticateEndpoint);
    }

    @Bean
    public OauthClientRoute oauthClientRoute() {
        return new OauthClientWebClient(webClientWithTokenFilter, authServerClientsEndpoint);
    }

    /*@Bean
    public OauthClientHandler oauthClientHandler() {
        return new OauthClientWebHandler(oauthClientRoute());
    }*/

    @Bean
    public AuthenticationCallout authenticationCallout() {
        return new AuthenticationCallout(webClientWithTokenFilter, springAuthorizationServerAuthenticationEp);
    }

    @Bean
    public OrganizationWebClient organizationWebClient() {
        return new OrganizationWebClient(webClientWithTokenFilter, organizationEndpoint);
    }
}
