package me.sonam.authzmanager.tokenfilter;

import com.netflix.discovery.converters.Auto;
import jakarta.servlet.http.HttpServletRequest;
import me.sonam.authzmanager.webclients.TokenWebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TokenService {

    private static final Logger LOG = LoggerFactory.getLogger(TokenService.class);

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;


    @Autowired
    private OAuth2AuthorizedClientManager authorizedClientManager;


    public String getAccessToken() {
        LOG.info("getAccessToken");
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            OAuth2AccessToken oAuth2AccessToken = getAccessToken(authentication);
            if (oAuth2AccessToken != null) {
                String accessToken = oAuth2AccessToken.getTokenValue();

                LOG.debug("returning access token to authenticated service code");
                return accessToken;
            }
            else {
                LOG.error("oAuth2AccessToken is null, return null");
                return null;
            }
        }
        else {
            LOG.error("authentication is null, return null for accessToken");
            return null;
        }
    }

    public OAuth2AccessToken getAccessToken(Authentication authentication) {
        var authorizedClient = this.getAuthorizedClient(authentication);
        LOG.debug("authorized client available: {}", authorizedClient != null);
        if (authorizedClient != null) {
            OAuth2AccessToken accessToken = authorizedClient.getAccessToken();

            if (accessToken != null) {
                LOG.info("accessToken is not null, returning accessToken object");
                Instant.now();
                if (accessToken.getExpiresAt().isBefore(Instant.now())) {
                    LOG.info("access token has expired, get a new refresh token");

                    throw new TokenExpiredException("token has expired");
                }
                return accessToken;
            }
            else {
                LOG.info("access token object is null");
            }
        }
        else {
            LOG.error("authorized client is null");
            throw new TokenExpiredException("Token is expired");
        }
        return null;
    }

    public OAuth2RefreshToken getRefreshToken(Authentication authentication) {
        var authorizedClient = this.getAuthorizedClient(authentication);
        if (authorizedClient != null) {
            OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();
            if (refreshToken != null) {
                return refreshToken;
            }
        }
        return null;
    }

    private OAuth2AuthorizedClient getAuthorizedClient(Authentication authentication) {
        LOG.debug("get OAuth2 authorized client for authenticated request");
        if (authentication instanceof OAuth2AuthenticationToken) {
            LOG.info("is Oauth2AuthenticationToken type");
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            LOG.info("got oauthToken");
            String clientRegistrationId = oauthToken.getAuthorizedClientRegistrationId();
            String principalName = oauthToken.getName();
            LOG.info("load OAuth2 authorized client for registration {}", clientRegistrationId);
            OAuth2AuthorizedClient oAuth2AuthorizedClient = authorizedClientService
                    .loadAuthorizedClient(clientRegistrationId, principalName);
            LOG.debug("OAuth2 authorized client loaded: {}", oAuth2AuthorizedClient != null);
            return oAuth2AuthorizedClient;
        }
        else {
            LOG.error("returning null because authentication is not an OAuth2AuthenticationToken");
        }
        return null;
    }
}
