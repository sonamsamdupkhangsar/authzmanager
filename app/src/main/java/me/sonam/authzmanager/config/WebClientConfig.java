package me.sonam.authzmanager.config;


import me.sonam.authzmanager.tenant.TenantAuthorizationUrlResolver;
import me.sonam.authzmanager.tokenfilter.TokenRequestFilter;
import me.sonam.authzmanager.tokenfilter.TokenFilter;
import me.sonam.authzmanager.tokenfilter.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configures shared WebClient builders, including the tenant-aware authorization-server client.
 */
@Profile("!localdevtest")
@Configuration
public class WebClientConfig {
    private static final Logger LOG = LoggerFactory.getLogger(WebClientConfig.class);
    @Value("${auth-server.root}${auth-server.contextPath}${auth-server.oauth2token.path}")
    private String oauth2TokenEndpoint;
    @Value("${auth-server.oauth2token.grantType}")
    private String grantType;
    @Value("${auth-server.oauth2token.issuerTokenPath:}")
    private String accessTokenPath;
    @Autowired
    private TokenRequestFilter tokenRequestFilter;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private TenantAuthorizationUrlResolver tenantAuthorizationUrlResolver;

    @Value("${tokenExpireSeconds}")
    private int tokenExpireSeconds;


    /**
     * Creates the plain load-balanced builder for non-auth-server service calls.
     */
    @LoadBalanced
    @Bean("regular")
    public WebClient.Builder webClientBuilder() {
        LOG.info("returning load balanced webclient part");
        return WebClient.builder();
    }

    /**
     * Creates the load-balanced builder used internally by the token filter for token acquisition.
     */
    @LoadBalanced
    @Bean("tokenFilter")
    public WebClient.Builder webClientBuilderForTokenFilter() {
        LOG.info("returning load balanced webclient part");
        return WebClient.builder();
    }

    /**
     * Creates the generic WebClient builder that attaches client-credentials tokens.
     */
    @LoadBalanced
    @Bean("webClientWithTokenFilter")
    public WebClient.Builder webClientBuilderNoFilter() {
        LOG.info("creating a WebClient.Builder with tokenFilter set");
        TokenFilter tokenFilter = new TokenFilter(webClientBuilderForTokenFilter(), tokenRequestFilter,
                oauth2TokenEndpoint, grantType, accessTokenPath, tokenExpireSeconds, tokenService);
        WebClient.Builder webClientBuilder = WebClient.builder();
        webClientBuilder.filter(tokenFilter.renewTokenFilter()).build();

        return webClientBuilder;
    }

    /**
     * Creates the authorization-server WebClient builder that forwards the tenant host on every request.
     */
    @Bean("authServerWebClient")
    public WebClient.Builder authServerWebClientBuilder() {
        LOG.info("creating a WebClient.Builder with tokenFilter and tenant auth-server headers");
        WebClient.Builder tokenRequestBuilder = webClientBuilderForTokenFilter().clone()
                .filter((request, next) -> next.exchange(ClientRequest.from(request)
                        .headers(tenantAuthorizationUrlResolver::applyTenantForwardHeaders)
                        .build()));

        TokenFilter tokenFilter = new TokenFilter(tokenRequestBuilder, tokenRequestFilter,
                oauth2TokenEndpoint, grantType, accessTokenPath, tokenExpireSeconds, tokenService);

        return webClientBuilder().clone()
                .filter(tokenFilter.renewTokenFilter())
                .filter((request, next) -> next.exchange(ClientRequest.from(request)
                        .headers(tenantAuthorizationUrlResolver::applyTenantForwardHeaders)
                        .build()));
    }
}
