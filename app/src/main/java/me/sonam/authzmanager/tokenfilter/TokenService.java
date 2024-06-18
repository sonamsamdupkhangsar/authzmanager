package me.sonam.authzmanager.tokenfilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private static final Logger LOG = LoggerFactory.getLogger(TokenService.class);

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    //@PreAuthorize("hasAuthority('SCOPE_profile')")
    private String getJwtToken() {
        LOG.info("get jwt token");
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var accessToken = getAccessToken(authentication);
        var refreshToken = getRefreshToken(authentication);
      /*  return String.format("Access Token = %s <br>",
                accessToken.getTokenValue());//, refreshToken.getTokenValue());*/

        String refreshTokenValue = null;
        if (refreshToken != null) {
            refreshTokenValue = refreshToken.getTokenValue();
        }
        return String.format("Access Token = %s <br><br><br> Refresh Token = %s",
                accessToken.getTokenValue(), refreshTokenValue);
    }

    public String getAccessToken() {
        LOG.info("getAccessToken");
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            OAuth2AccessToken oAuth2AccessToken = getAccessToken(authentication);
            if (oAuth2AccessToken != null) {
                String accessToken = oAuth2AccessToken.getTokenValue();

                LOG.info("accessToken: {}", accessToken);
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
        LOG.info("authorizedClient: {}", authorizedClient);
        if (authorizedClient != null) {
            OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
            LOG.info("accessToken: {}", accessToken);
            if (accessToken != null) {
                return accessToken;
            }
        }
        else {
            LOG.error("authorizedClient is null, authentication: {}", authentication);
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
        LOG.info("get Oauth2AuthorizedClient: {}", authentication);
        if (authentication instanceof OAuth2AuthenticationToken) {
            LOG.info("is Oauth2AuthenticationToken type");
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            LOG.info("got oauthToken");
            String clientRegistrationId = oauthToken.getAuthorizedClientRegistrationId();
            String principalName = oauthToken.getName();
            LOG.info("returning OAuth2AuthorizedClient: clientRegistrationId: {}, principalName: {}",
                    clientRegistrationId, principalName);
            OAuth2AuthorizedClient oAuth2AuthorizedClient = authorizedClientService
                    .loadAuthorizedClient(clientRegistrationId, principalName);
            LOG.info("oauth2AuthorizedClient to return: {}", oAuth2AuthorizedClient);
            return oAuth2AuthorizedClient;
        }
        else {
            LOG.error("returning null, authentication is not an instanceof OAuth2AuthenticationToken: {}", authentication);
        }
        return null;
    }
}