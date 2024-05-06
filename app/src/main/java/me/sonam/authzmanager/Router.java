package me.sonam.authzmanager;

import me.sonam.authzmanager.clients.OauthClientHandler;
import me.sonam.authzmanager.user.UserHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

/**
 * Set AccountService methods route for checking active and to actiate acccount
 */
//@Configuration
public class Router {
    private static final Logger LOG = LoggerFactory.getLogger(Router.class);

/*    @Bean
    public RouterFunction<ServerResponse> route(OauthClientHandler handler, UserHandler userHandler) {
        LOG.info("building authenticate router function");
        return RouterFunctions.route(POST("/authzmanager/authenticate").and(accept(MediaType.APPLICATION_JSON)),
                userHandler::authenticate)
                .andRoute(POST("/authzmanager").and(accept(MediaType.APPLICATION_JSON)),
                        userHandler::createUser)
                .andRoute(POST("/authzmanager/clients").and(accept(MediaType.APPLICATION_JSON)),
                        handler::createClient)
                .andRoute(PUT("/authzmanager/clients").and(accept(MediaType.APPLICATION_JSON)),
                        handler::updateClient)
                .andRoute(POST("/authzmanager/clients/{userId}").and(accept(MediaType.APPLICATION_JSON)),
                        handler::getUserOauth2Clients)
                .andRoute(DELETE("/authzmanager/clients/{clientId}").and(accept(MediaType.APPLICATION_JSON)),
                        handler::deleteClient);
    }*/
}
