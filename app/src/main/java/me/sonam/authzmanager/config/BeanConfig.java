package me.sonam.authzmanager.config;

import me.sonam.authzmanager.AuthenticationCallout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class BeanConfig {
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;
    @Autowired
    private WebClient.Builder webClientBuilder;

    @Value("${auth-server.root}${auth-server.authenticate}")
    private String springAuthorizationServerAuthenticationEp;

    @Bean
    public AuthenticationCallout authenticationCallout() {
        return new AuthenticationCallout(webClientBuilder, springAuthorizationServerAuthenticationEp);
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        return ReactiveJwtDecoders.fromIssuerLocation(issuerUri);
    }
}
