package me.sonam.authzmanager;

import me.sonam.authzmanager.tokenfilter.TokenRequestFilter;
import me.sonam.authzmanager.tokenfilter.TokenFilter;
import me.sonam.authzmanager.tokenfilter.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;
@ComponentScan("cloud.sonam")

@Profile("localdevtest")
@Configuration
public class WebClientDevConfig {
    @Value("${auth-server.root}${auth-server.oauth2token.path}${auth-server.oauth2token.params:}")
    private String oauth2TokenEndpoint;
    @Value("${auth-server.oauth2token.path:}")
    private String accessTokenPath;
    @Value("${auth-server.oauth2token.grantType}")
    private String grantType;

    @Autowired
    private TokenRequestFilter tokenRequestFilter;

    @Autowired
    private TokenService tokenService;

    private static final Logger LOG = LoggerFactory.getLogger(WebClientDevConfig.class);

    @Bean("regular")
    public WebClient.Builder webClientBuilder() {
        LOG.info("returning non-loadbalanced webclient");

        return WebClient.builder();
    }

    @Bean("tokenFilter")
    public WebClient.Builder webClientBuilderForTokenFilter() {
        LOG.info("returning load balanced webclient part");
        return WebClient.builder();
    }

    @Bean("webClientWithTokenFilter")
    public WebClient.Builder webClientBuilderNoFilter() {
        LOG.info("creating a WebClient.Builder with tokenFilter set");
        TokenFilter tokenFilter = new TokenFilter(webClientBuilderForTokenFilter(), tokenRequestFilter, oauth2TokenEndpoint, grantType, accessTokenPath);
        WebClient.Builder webClientBuilder = WebClient.builder();
        webClientBuilder.filter(tokenFilter.renewTokenFilter()).build();

        return webClientBuilder;
    }
}
