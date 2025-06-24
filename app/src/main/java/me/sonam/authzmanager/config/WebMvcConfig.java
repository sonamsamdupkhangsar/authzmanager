package me.sonam.authzmanager.config;

import me.sonam.authzmanager.interceptor.UserIdCheckInterceptor;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.webclients.UserWebClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserWebClient userWebClient;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //registry.addInterceptor(new UserIdCheckInterceptor(tokenService, userWebClient));
    }
}