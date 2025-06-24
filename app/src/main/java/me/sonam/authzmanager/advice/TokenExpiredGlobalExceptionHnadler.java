package me.sonam.authzmanager.advice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import me.sonam.authzmanager.interceptor.InvalidUserIdException;
import me.sonam.authzmanager.tokenfilter.TokenExpiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class TokenExpiredGlobalExceptionHnadler {
    private static final Logger LOG = LoggerFactory.getLogger(TokenExpiredGlobalExceptionHnadler.class);

    @ExceptionHandler({TokenExpiredException.class, InvalidUserIdException.class})
    public String handleAuthenticationException(HttpServletRequest request) {
        LOG.info("handle tokenExpiredException");
        // Invalidate the session
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // Clear the security context
        SecurityContextHolder.clearContext();

        // Optionally, redirect the user to the login page or a custom logout page
        return "redirect:/login?logout";
    }
}
