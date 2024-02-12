package me.sonam.authzmanager;

import me.sonam.authzmanager.clients.OauthClientRoute;
import me.sonam.authzmanager.clients.OauthClientRouteRouteAuthServer;
import me.sonam.authzmanager.tokenfilter.JwtPath;
import me.sonam.authzmanager.tokenfilter.TokenFilter;
import me.sonam.authzmanager.user.UserRoute;
import me.sonam.authzmanager.user.UserRouteAuthServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

@Profile("localdevtest")
@Configuration
public class WebClientDevConfig {
    private static final Logger LOG = LoggerFactory.getLogger(WebClientDevConfig.class);

    @Value("{user-rest-service.root}${user-rest-service.signup}")
    private String userSignupEndpoint;

    @Value("{authentication-rest-service.root}${authentication-rest-service.authenticate}")
    private String authenticateEndpoint;
    @Value("${auth-server.root}${auth-server.clients}")
    private String authServerClientsEndpoint;
    @Value("${auth-server.root}${auth-server.authenticate}")
    private String springAuthorizationServerAuthenticationEp;
    @Value("${auth-server.root}${auth-server.oauth2token.path}${auth-server.oauth2token.params:}")
    private String oauth2TokenEndpoint;

    @Autowired
    private JwtPath jwtPath;

    @Value("${auth-server.oauth2token.path:}")
    private String accessTokenPath;

    @Bean
    public WebClient.Builder webClientBuilder() {
        LOG.info("returning non-loadbalanced webclient");
        return WebClient.builder();
    }

    @Bean("noFilter")
    public WebClient.Builder webClientBuilderNoFilter() {
        LOG.info("returning for noFilter load balanced webclient part");
        return WebClient.builder();
    }


    @Bean
    public UserRoute userRoute() {
        return new UserRouteAuthServer(webClientBuilder(), userSignupEndpoint, authenticateEndpoint);
    }

    @Bean
    public OauthClientRoute oauthClientRoute() {
        return new OauthClientRouteRouteAuthServer(webClientBuilder(), authServerClientsEndpoint);
    }

    @Bean
    public AuthenticationCallout authenticationCallout() {
        WebClient.Builder webCliBuilder = webClientBuilder();
        webCliBuilder.filter(tokenFilter().renewTokenFilter()).build();
        return new AuthenticationCallout(webCliBuilder, springAuthorizationServerAuthenticationEp);
    }

    @Bean
    @DependsOn("noFilter")
    public TokenFilter tokenFilter() {
        return new TokenFilter(webClientBuilderNoFilter(), jwtPath, oauth2TokenEndpoint, accessTokenPath);
    }

}
