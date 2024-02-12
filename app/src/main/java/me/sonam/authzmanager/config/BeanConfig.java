package me.sonam.authzmanager.config;

import me.sonam.authzmanager.tokenfilter.TokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class BeanConfig {
/*    @Bean
    @DependsOn("noFilter")
    public TokenFilter tokenFilter() {
        return new TokenFilter(webClientBuilder(), jwtPath, oauth2TokenEndpoint, accessTokenPath);
    }*/
}
