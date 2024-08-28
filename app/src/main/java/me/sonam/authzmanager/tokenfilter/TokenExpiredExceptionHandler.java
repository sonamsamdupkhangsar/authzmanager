package me.sonam.authzmanager.tokenfilter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;

@Component
public class TokenExpiredExceptionHandler extends AbstractHandlerExceptionResolver {
    private static final Logger LOG = LoggerFactory.getLogger(TokenExpiredExceptionHandler.class);

    @Override
    protected ModelAndView doResolveException(HttpServletRequest request, HttpServletResponse response,
                                              Object handler, Exception ex) {
        LOG.warn("exception occurred", ex);

        if (ex instanceof TokenExpiredException ||
                ex.getMessage().contains("401 Unauthorized from ")) {
            LOG.info("token has expired, logout user");

            try {
                LOG.info("use response object to redirect to /logout");
                response.sendRedirect("/logout");
            }
            catch (Exception e) {
                LOG.error("redirect to logout path caused exception", e);
            }
        }

        LOG.info("return null for other exceptions");
        return null;
    }
}
