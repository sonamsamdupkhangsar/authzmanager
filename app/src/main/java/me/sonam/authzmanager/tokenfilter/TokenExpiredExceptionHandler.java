package me.sonam.authzmanager.tokenfilter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
            return new ModelAndView("/userlogout");
        }

        if (ex.getMessage().contains("401 Unauthorized from ")) {
            LOG.info("logging user out programmatically when it is a http 401 exception. ");
            LOG.info("this can happen if the token has expired");

            return new ModelAndView("/userlogout");
        }

        return null;
    }
}
