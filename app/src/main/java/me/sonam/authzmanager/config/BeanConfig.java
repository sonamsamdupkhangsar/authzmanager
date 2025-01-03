package me.sonam.authzmanager.config;

import me.sonam.authzmanager.AuthenticationCallout;
import me.sonam.authzmanager.user.UserRoute;
import me.sonam.authzmanager.user.UserRouteAuthServer;
import me.sonam.authzmanager.webclients.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class BeanConfig {
    @Value("${user-rest-service.root}${user-rest-service.context}")
    private String userSignupEndpoint;

    @Value("${user-rest-service.root}${user-rest-service.context}${user-rest-service.photo}")
    private String userProfilePhoto;

    @Value("${authentication-rest-service.root}${authentication-rest-service.authenticate}")
    private String authenticateEndpoint;
    @Value("${auth-server.root}${auth-server.contextPath}${auth-server.clients}")
    private String authServerClientsEndpoint;

    @Value("${auth-server.root}${auth-server.contextPath}${auth-server.clientOrganizations}")
    private String authServerClientOrganizationsEndpoint;

    @Value("${auth-server.root}${auth-server.contextPath}${auth-server.authenticate}")
    private String springAuthorizationServerAuthenticationEp;

    @Value("${organization-rest-service.root}${organization-rest-service.contextPath}")
    private String organizationEndpoint;

    @Value("${role-rest-service.root}${role-rest-service.contextPath}")
    private String rolesEndpoint;

    @Value("${account-rest-service.accountDelete}")
    private String deleteMyAccountEndpoint;

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
    public OauthClientWebClient oauthClientWebClient() {
        return new OauthClientWebClient(webClientWithTokenFilter, authServerClientsEndpoint);
    }


    @Bean
    public AuthenticationCallout authenticationCallout() {
        return new AuthenticationCallout(webClientWithTokenFilter, springAuthorizationServerAuthenticationEp);
    }

    @Bean
    public OrganizationWebClient organizationWebClient() {
        return new OrganizationWebClient(webClientWithTokenFilter, organizationEndpoint);
    }

    @Bean
    public RoleWebClient roleWebClient() {
        return new RoleWebClient(webClientWithTokenFilter, rolesEndpoint);
    }

    @Bean
    public UserWebClient userWebClient() {
        return new UserWebClient(webClientWithTokenFilter, userSignupEndpoint, userProfilePhoto);
    }

    @Bean
    public ClientOrganizationWebClient clientOrganizationWebClient() {
        return new ClientOrganizationWebClient(webClientWithTokenFilter, authServerClientOrganizationsEndpoint);
    }

    @Bean
    public AccountWebClient accountWebClient() {
        return new AccountWebClient(webClientWithTokenFilter, deleteMyAccountEndpoint);
    }
}
