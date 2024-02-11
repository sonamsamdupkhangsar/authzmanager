package me.sonam.authzmanager;

import me.sonam.authzmanager.clients.OauthClientRoute;
import me.sonam.authzmanager.clients.OauthClientRouteRouteAuthServer;
import me.sonam.authzmanager.user.UserRoute;
import me.sonam.authzmanager.user.UserRouteAuthServer;
import me.sonam.security.headerfilter.ReactiveRequestContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    public ReactiveRequestContextHolder reactiveRequestContextHolder() {
        WebClient.Builder webClientBuilder = webClientBuilderNoFilter();
        ReactiveRequestContextHolder reactiveRequestContextHolder = new ReactiveRequestContextHolder(webClientBuilder);
        webClientBuilder.filter(reactiveRequestContextHolder.headerFilter());

        return reactiveRequestContextHolder;
    }

    @Bean
    public UserRoute userRoute() {
        return new UserRouteAuthServer(webClientBuilder(), userSignupEndpoint, authenticateEndpoint);
    }

    @Bean
    public OauthClientRoute oauthClientRoute() {
        return new OauthClientRouteRouteAuthServer(webClientBuilder(), authServerClientsEndpoint);
    }
}
