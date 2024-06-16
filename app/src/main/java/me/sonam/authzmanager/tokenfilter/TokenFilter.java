package me.sonam.authzmanager.tokenfilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * this token filter will be invoked automatically by the webclient for intercepting request
 * to add a access-token by making a client-credential flow http call.
 * Don't add it manually to a webclient to avoid getting calling twice.
 * This is copied from authorization server project
 */
public class TokenFilter {
    private static final Logger LOG = LoggerFactory.getLogger(TokenFilter.class);

    private String oauth2TokenEndpoint;
    private String grantType;
   // @Autowired
    private JwtPath jwtPath;

    private WebClient.Builder webClientBuilder;

    private TokenService tokenService;

    public TokenFilter(WebClient.Builder webClientBuilder, JwtPath jwtPath, String oauth2TokenEndpoint, String grantType, TokenService tokenService) {
        this.webClientBuilder = webClientBuilder;
        this.grantType = grantType;
        this.jwtPath = jwtPath;
        this.oauth2TokenEndpoint = oauth2TokenEndpoint;
        this.tokenService = tokenService;
    }

    public ExchangeFilterFunction renewTokenFilter() {
        return (request, next) -> {
            LOG.debug("request.path: {}", request.url().getPath());

            LOG.info("going thru jwt request ") ;
            for (JwtPath.JwtRequest jwt : jwtPath.getJwtRequest()) {
                LOG.debug("jwt.out: {}", jwt.getOut());
                String[] outMatches = jwt.getOut().split(",");

                for (String outPath : outMatches) {
                    LOG.debug("outPath: {}", outPath);
                    if (request.url().getPath().matches(outPath.trim())) {
                        LOG.info("make a token request for path {} matches with outbound request matches: {}",
                                outPath, request.url().getPath());

                        return getClientRequest(request, next, jwt, outPath);
                    }
                }
            }

            LOG.info("no outbound path match found, not adding any auth token");
            ClientRequest filtered = ClientRequest.from(request).build();
            return next.exchange(filtered);
        };
    }

    private Mono<ClientResponse> getClientRequest(ClientRequest request, ExchangeFunction next, JwtPath.JwtRequest jwt, String outPath) {
        LOG.info("clientRequest");
        if (jwt.getAccessToken().getOption().equals(JwtPath.JwtRequest.AccessToken.JwtOption.forward)) {
            LOG.error("forward request as is");
            ClientRequest filtered = ClientRequest.from(request)
                    .build();
            return next.exchange(filtered);
            /*return ReactiveSecurityContextHolder.getContext()
                    .map(SecurityContext::getAuthentication)

                    .flatMap(authentication -> {

                        LOG.info("authentication: {}", authentication);
                if (authentication != null) {
                    LOG.info("authentication: {}", authentication);

                    final OAuth2AccessToken oAuth2AccessToken = tokenService.getAccessToken(authentication);
                    LOG.info("oauth2AccessToken: {}", oAuth2AccessToken);

                    ClientRequest clientRequest = ClientRequest.from(request)
                            .headers(headers -> {
                                headers.set(HttpHeaders.ORIGIN, request.headers().getFirst(HttpHeaders.ORIGIN));
                                headers.setBearerAuth(oAuth2AccessToken.getTokenValue());
                                LOG.info("added users access-token to http header: {}, tokenType: {}",
                                        oAuth2AccessToken.getTokenValue(), oAuth2AccessToken.getTokenType());
                            }).build();

                    return next.exchange(clientRequest);
                }
                else {
                    LOG.error("authentication is null, forward request as is");
                    ClientRequest filtered = ClientRequest.from(request)
                            .build();
                    return next.exchange(filtered);
                }
            });*/
        }
        else if (jwt.getAccessToken().getOption().equals(JwtPath.JwtRequest.AccessToken.JwtOption.request)) {
            return getAccessToken(oauth2TokenEndpoint.toString(), grantType, jwt.getAccessToken().getScopes(), jwt.getAccessToken().getBase64EncodedClientIdSecret())
                    .flatMap(accessToken -> {
                        LOG.info("got accessToken using client-credential: {}", accessToken);
                        ClientRequest clientRequest = ClientRequest.from(request)
                                .headers(headers -> {
                                    headers.set(HttpHeaders.ORIGIN, request.headers().getFirst(HttpHeaders.ORIGIN));
                                    headers.setBearerAuth(accessToken);
                                    LOG.info("added access-token to http header");
                                }).build();
                        return Mono.just(clientRequest);
                    }).flatMap(clientRequest -> next.exchange(clientRequest));
        }
        else {
            LOG.info("not going to request a token, forward the request with a Auth token");
            ClientRequest filtered = ClientRequest.from(request)
                    .build();
            return next.exchange(filtered);
        }
    }
    private Mono<String> getAccessToken(final String oauthEndpoint, String grantType, String scopes, final String base64EncodeClientIdSecret) {
        LOG.info("making a access-token request to endpoint: {}",oauthEndpoint);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("grant_type", grantType);

        List<String> scopeList = Arrays.stream(scopes.split(" ")).toList();
        body.add("scopes", scopeList);

        LOG.debug("add body payload for grant type and scopes");

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().post().uri(oauthEndpoint)
                .bodyValue(body)
                .headers(httpHeaders -> httpHeaders.setBasicAuth(base64EncodeClientIdSecret))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve();

        return responseSpec.bodyToMono(Map.class).map(map -> {
            LOG.debug("response for '{}' is in map: {}", oauthEndpoint, map);
            if (map.get("access_token") != null) {
                return map.get("access_token").toString();
            }
            else {
                LOG.error("nothing to return");
                return "nothing";
            }
        }).onErrorResume(throwable -> {
            LOG.error("client credentials access token rest call failed: {}", throwable.getMessage());
            String errorMessage = throwable.getMessage();

            if (throwable instanceof WebClientResponseException) {
                WebClientResponseException webClientResponseException = (WebClientResponseException) throwable;
                LOG.error("error body contains: {}", webClientResponseException.getResponseBodyAsString());
                errorMessage = webClientResponseException.getResponseBodyAsString();
            }
            return Mono.error(new RuntimeException(errorMessage));
        });
    }
}
