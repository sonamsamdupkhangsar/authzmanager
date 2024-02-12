package me.sonam.authzmanager.config;

import jakarta.annotation.PostConstruct;

import me.sonam.authzmanager.AuthenticationCallout;
import me.sonam.authzmanager.tokenfilter.TokenFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientFilterConfig {
    private static final Logger LOG = LoggerFactory.getLogger(WebClientFilterConfig.class);

    @Autowired
    private WebClient.Builder webCliBuilder;

    @Autowired
    private TokenFilter tokenFilter;
    @Value("${auth-server.root}${auth-server.authenticate}")
    private String springAuthorizationServerAuthenticationEp;

    @PostConstruct
    public void addFilterToWebClient() {
        LOG.info("configure the renewTokenFilter only once in this config");
        webCliBuilder.filter(tokenFilter.renewTokenFilter()).build();
    }

    @Bean
    public AuthenticationCallout authenticationCallout() {
        return new AuthenticationCallout(webCliBuilder, springAuthorizationServerAuthenticationEp, tokenFilter);
    }
}
