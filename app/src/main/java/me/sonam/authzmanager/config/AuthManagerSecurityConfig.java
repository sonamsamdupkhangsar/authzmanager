package me.sonam.authzmanager.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * this is the security configuration to use OAuth2 for login and permit access to health endpoints, signup.
 */
@Configuration
@EnableWebSecurity
public class AuthManagerSecurityConfig {
    private static final Logger LOG = LoggerFactory.getLogger(AuthManagerSecurityConfig.class);

    @Value("${allowedOrigins}")
    private String allowedOrigins; //csv allow origins

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authorize) ->
                        authorize.requestMatchers("/actuator/**").permitAll()
                                .requestMatchers("/api/health/readiness").permitAll()
                                .requestMatchers("/signup").permitAll()
                                .requestMatchers("/").permitAll()
                                .anyRequest().authenticated()
                )
                .csrf(AbstractHttpConfigurer::disable)
               .oauth2Login(Customizer.withDefaults());

        return http.cors(Customizer.withDefaults()).build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        allowedOrigins = allowedOrigins.replace(" ", ""); //remove whitespaces between csv
        List<String> list = Arrays.asList(allowedOrigins.split(","));
        LOG.info("adding allowedOrigins: {}", list);

        corsConfig.setAllowedOrigins(list);
        corsConfig.setAllowedMethods(Arrays.asList("GET", "PUT", "POST", "OPTIONS"));
        corsConfig.addAllowedHeader("*");
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        return source;
    }
}