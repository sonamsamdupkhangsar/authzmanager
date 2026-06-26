package me.sonam.authzmanager.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

@Profile("non-eureka")
@Configuration
public class DirectWebClientBuilderConfig {
    private static final Logger LOG = LoggerFactory.getLogger(DirectWebClientBuilderConfig.class);

    @Bean("regular")
    public WebClient.Builder webClientBuilder() {
        LOG.info("creating direct service WebClient for non-Eureka service discovery");
        return WebClient.builder();
    }

    @Bean("tokenFilter")
    public WebClient.Builder webClientBuilderForTokenFilter() {
        LOG.info("creating direct token WebClient for non-Eureka service discovery");
        return WebClient.builder();
    }
}
