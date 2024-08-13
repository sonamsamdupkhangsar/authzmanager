package me.sonam.authzmanager.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;


import java.io.IOException;

public class CustomOAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {
    private static final Logger LOG = LoggerFactory.getLogger(CustomOAuth2AuthenticationFailureHandler.class);

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        // Handle the error here
        // You can log the error, redirect to a custom error page, or return a JSON response
        // For example, redirect to a custom error page:
        LOG.error("error occurred during login", exception);

        response.sendRedirect("/oauth2-login-error");
    }


}