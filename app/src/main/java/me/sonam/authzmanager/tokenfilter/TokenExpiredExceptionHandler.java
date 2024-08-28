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
        LOG.warn("exception occured", ex);

        if (ex instanceof TokenExpiredException) {
            LOG.info("token has expired, logout user");

            //return new ModelAndView("/logout");
            try {
                LOG.info("use response object to redirect to /logout");
                response.sendRedirect("/logout");
            }
            catch (Exception e) {
                LOG.error("redirect to logout path caused exception", e);
            }
            return new ModelAndView("/userlogout")
        }

        if (ex.getMessage().contains("401 Unauthorized from ")) {
            LOG.info("logging user out programmatically when it is a http 401 exception.");
            LOG.info("this can happen if the token has expired");

            new SecurityContextLogoutHandler().logout(request, null, null);
            return new ModelAndView("/logout");
        }

        return null;
    }
}
