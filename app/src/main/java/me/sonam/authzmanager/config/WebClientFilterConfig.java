package me.sonam.authzmanager.config;

import jakarta.annotation.PostConstruct;

import me.sonam.authzmanager.AuthenticationCallout;
import me.sonam.authzmanager.tokenfilter.JwtPath;
import me.sonam.authzmanager.tokenfilter.TokenFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

//@Configuration
public class WebClientFilterConfig {
    private static final Logger LOG = LoggerFactory.getLogger(WebClientFilterConfig.class);

    @Autowired
    @Qualifier("noFilter")
    private WebClient.Builder noFilterWebClient;

    @Value("${auth-server.root}${auth-server.oauth2token.path}${auth-server.oauth2token.params:}")
    private String oauth2TokenEndpoint;

    @Autowired
    private JwtPath jwtPath;

     @Value("${auth-server.oauth2token.path:}")
    private String accessTokenPath;

    /*@PostConstruct
    public void addFilterToWebClient() {
        LOG.info("configure the renewTokenFilter only once in this config");
        webCliBuilder.filter(tokenFilter.renewTokenFilter()).build();
    }*/


    @Bean
    public TokenFilter tokenFilter() {
        return new TokenFilter(noFilterWebClient, jwtPath, oauth2TokenEndpoint, accessTokenPath);
    }
}
