package me.sonam.authzmanager;

import jakarta.annotation.PostConstruct;
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
    @Value("${auth-server.root}${auth-server.oauth2token.path}${auth-server.oauth2token.params:}")
    private String oauth2TokenEndpoint;
    @Value("${auth-server.oauth2token.path:}")
    private String accessTokenPath;
    @Autowired
    private JwtPath jwtPath;
    private static final Logger LOG = LoggerFactory.getLogger(WebClientDevConfig.class);

    @Bean("regular")
    public WebClient.Builder webClientBuilder() {
        LOG.info("returning non-loadbalanced webclient");

        return WebClient.builder();
    }

    @Bean("webClientWithTokenFilter")
    public WebClient.Builder webClientBuilderNoFilter() {
        LOG.info("creating a WebClient.Builder with tokenFilter set");
        TokenFilter tokenFilter = new TokenFilter(WebClient.builder(), jwtPath, oauth2TokenEndpoint);
        WebClient.Builder webClientBuilder = WebClient.builder();
        webClientBuilder.filter(tokenFilter.renewTokenFilter()).build();

        return webClientBuilder;
    }


}
