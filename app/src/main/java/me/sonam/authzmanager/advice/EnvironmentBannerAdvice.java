package me.sonam.authzmanager.advice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Locale;

@ControllerAdvice
public class EnvironmentBannerAdvice {
    private static final String TEST_DOMAIN = ".test";

    @ModelAttribute("testEnvironment")
    public boolean testEnvironment(HttpServletRequest request) {
        return currentHost(request).endsWith(TEST_DOMAIN);
    }

    @ModelAttribute("environmentBanner")
    public String environmentBanner(HttpServletRequest request) {
        String host = currentHost(request);
        return host.endsWith(TEST_DOMAIN) ? "LOCAL TEST ENVIRONMENT - " + host : null;
    }

    private String currentHost(HttpServletRequest request) {
        String host = request.getHeader("X-Forwarded-Host");
        if (host == null || host.isBlank()) {
            host = request.getServerName();
        }
        if (host == null) {
            return "";
        }
        host = host.split(",", 2)[0].trim().toLowerCase(Locale.ROOT);
        int portIndex = host.indexOf(':');
        return portIndex >= 0 ? host.substring(0, portIndex) : host;
    }
}
