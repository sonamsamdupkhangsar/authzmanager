package me.sonam.authzmanager.tokenfilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/**
 * this token filter will be invoked automatically by the webclient for intercepting request
 * to add a access-token by making a client-credential flow http call.
 * Don't add it manually to a webclient to avoid getting calling twice.
 * This is copied from authorization server project
 */
public class TokenFilter {
    private static final Logger LOG = LoggerFactory.getLogger(TokenFilter.class);

    private final String oauth2TokenEndpoint;

    private final String grantType;
    private final String accessTokenPath;
    private final TokenRequestFilter tokenRequestFilter;

    private final WebClient.Builder webClientBuilder;

    private final int tokenExpireSeconds;
    private TokenService tokenService;

    public TokenFilter(WebClient.Builder webClientBuilder, TokenRequestFilter tokenRequestFilter,
                       String oauth2TokenEndpoint, String grantType, String accessTokenPath,
                       int tokenExpireSeconds, TokenService tokenService) {
        this.webClientBuilder = webClientBuilder;
        this.grantType = grantType;
        this.tokenRequestFilter = tokenRequestFilter;
        this.oauth2TokenEndpoint = oauth2TokenEndpoint;
        this.accessTokenPath = accessTokenPath;
        this.tokenExpireSeconds = tokenExpireSeconds;
        this.tokenService = tokenService;
    }

    public ExchangeFilterFunction renewTokenFilter() {
        return (request, next) -> {
            LOG.debug("accessing ReactiveSecurityContextHolder");
            return ReactiveSecurityContextHolder.getContext()
                    .map(SecurityContext::getAuthentication)
                    .flatMap(authentication -> {

                        LOG.info("authentication: {}", authentication);
                        if (authentication != null) {
                            LOG.info("authentication: {}", authentication);

                            final OAuth2AccessToken oAuth2AccessToken = tokenService.getAccessToken(authentication);
                            LOG.info("oauth2AccessToken: {}", oAuth2AccessToken.getTokenValue());

                            LOG.info("outbound request path: {}", request.url().getPath());
                            if (request.url().getPath().equals(accessTokenPath)) {
                                LOG.debug("no need to request access token when going to that path: {}", request.url().getPath());
                                ClientRequest clientRequest = ClientRequest.from(request).build();
                                return next.exchange(clientRequest);
                            }
                            else {
                                return processTokenFilter(oAuth2AccessToken.getTokenValue(), request, next);
                            }
                        }
                        else {
                            LOG.info("authentication is null in reactive context");
                            return processTokenFilter(null, request, next);
                        }
                    }).switchIfEmpty(processTokenFilter(null, request, next));
        };
    }

    private Mono<ClientResponse>  processTokenFilter(String inboundAuthorizationToken, ClientRequest request, ExchangeFunction next) {
        LOG.debug("going thru request filters, inboundAuthorizationToken null? : {}", inboundAuthorizationToken == null) ;
        int index = 0;

        for (TokenRequestFilter.RequestFilter requestFilter : tokenRequestFilter.getRequestFilters()) {
            LOG.info("checking requestFilter[{}]  {}", index++, requestFilter);

            if (!requestFilter.getOutHttpMethods().isEmpty()) {

                LOG.debug("httpMethods: {} provided, actual inbound httpMethod: {}", requestFilter.getOut(),
                        request.method().name());

                LOG.info("requestFilter.getInHttpMethodSet().contains(request.method().name().toLowerCase(): {}",
                        requestFilter.getOutHttpMethodSet().contains(request.method().name().toLowerCase()));

                if (requestFilter.getOutHttpMethods().contains(request.method().name().toLowerCase())) {
                    LOG.info("outbound request method {} matched with provided httpMethod", request.method().name());

                    boolean matchOutPath = requestFilter.getOutSet().stream().anyMatch(w -> {
                        boolean matched = request.url().getPath().matches(w);
                        if (LOG.isDebugEnabled() && matched) {
                            LOG.debug("outPath {} matched with regEx {}", request.url().getPath(), w);
                        }
                        return matched;
                    });

                    if (matchOutPath) {
                        LOG.info("outbound path matched");
                        try {
                            return passAccessToken(inboundAuthorizationToken, request, next, requestFilter.getAccessToken());
                        } catch (Exception e) {
                            LOG.error("Exception occurred, clear out access token", e);
                            requestFilter.getAccessToken().setAccessToken(null);
                            return Mono.error(e);
                        }
                    } else {
                        LOG.info("no match found for outbound path {} ", request.url().getPath());
                    }
                }
            }
            else if (requestFilter.getOutHttpMethods().isEmpty() && requestFilter.getOut().isEmpty()) {
                LOG.info("user request filter to apply a general filter when out http method and out path is empty");
                try {
                    return passAccessToken(inboundAuthorizationToken, request, next, requestFilter.getAccessToken());
                }
                catch (Exception e) {
                    LOG.error("Exception occurred, clear out access token", e);
                    requestFilter.getAccessToken().setAccessToken(null);
                    return Mono.error(e);
                }
            }
            else {
                LOG.error("either outbound request method and out path must be specified or leave them empty");
            }
        }
        LOG.info("no match found");
        ClientRequest filtered = ClientRequest.from(request).build();
        return next.exchange(filtered);
    }

    private Mono<ClientResponse> passAccessToken(String inboundAuthorizationToken, ClientRequest request, ExchangeFunction next,
                                                 TokenRequestFilter.RequestFilter.AccessToken accessToken) {
        LOG.info("pass inbound token, request or do nothing");

        if (accessToken.getOption().equals(TokenRequestFilter.RequestFilter.AccessToken.JwtOption.forward)) {
            LOG.info("option is forward token");
            return getClientRequestWithHeader(inboundAuthorizationToken, request, next);
        }
        else if (accessToken.getOption().equals(TokenRequestFilter.RequestFilter.AccessToken.JwtOption.request)) {
            if (inboundAuthorizationToken != null) {
                LOG.info("is a request for access-token but send existing inbound token");
                return getClientRequestWithHeader(inboundAuthorizationToken, request, next);
            }
            else {
                LOG.info("no inbound access-token");

                if (accessToken.getAccessToken() != null && !isExpired(accessToken.getAccessTokenCreationTime())) {
                    LOG.info("accessToken object contains a accessToken that is not expired");
                    return getClientRequestWithHeader(accessToken.getAccessToken(), request, next);

                }
                else {
                    LOG.info("accessToken.accessToken is null or expired");
                    return getAccessToken(oauth2TokenEndpoint, grantType, accessToken.getScopes(), accessToken.getBase64EncodedClientIdSecret())
                            .flatMap(jwtAccessToken -> {
                                LOG.info("set token in access-token");
                                accessToken.setAccessToken(jwtAccessToken);

                                ClientRequest clientRequest = getClientRequest(accessToken.getAccessToken(), request, next);
                                return Mono.just(clientRequest);
                            }).flatMap(next::exchange);
                }
            }
        } // there is no need to forward as there is no inbound token coming in, just requests going out
        else {
            LOG.info("do nothing");
            ClientRequest filtered = ClientRequest.from(request).build();
            return next.exchange(filtered);
        }
    }

    private Mono<ClientResponse> getClientRequestWithHeader(String accessToken, ClientRequest request, ExchangeFunction next) {
        ClientRequest clientRequest = getClientRequest(accessToken, request, next);
        return next.exchange(clientRequest);
    }

    private ClientRequest getClientRequest(String accessToken, ClientRequest request, ExchangeFunction next) {
        return ClientRequest.from(request)
                .headers(headers -> {
                    headers.set(HttpHeaders.ORIGIN, request.headers().getFirst(HttpHeaders.ORIGIN));
                    if (accessToken != null) {
                        headers.setBearerAuth(accessToken);
                        LOG.info("set authorization header with {}", accessToken);
                    }
                }).build();
    }

    private Mono<ClientResponse> getClientRequest(ClientRequest request, ExchangeFunction next, TokenRequestFilter.RequestFilter jwt) {
        LOG.info("getClientRequest()");
        if (jwt.getAccessToken().getOption().equals(TokenRequestFilter.RequestFilter.AccessToken.JwtOption.forward)) {
            LOG.info("forwarding with authorization header if present is {}", request.headers().get(AUTHORIZATION));
            ClientRequest filtered = ClientRequest.from(request)
                    .build();
            return next.exchange(filtered);
        }
        else if (jwt.getAccessToken().getOption().equals(TokenRequestFilter.RequestFilter.AccessToken.JwtOption.request)) {
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
            LOG.info("forward the request as is");
            ClientRequest filtered = ClientRequest.from(request)
                    .build();
            return next.exchange(filtered);
        }
    }

    private boolean isExpired(LocalDateTime tokenTime) {
        LocalDateTime tokenExpiredTime = LocalDateTime.now().minus(Duration.ofSeconds(tokenExpireSeconds));

        return tokenTime.isBefore(tokenExpiredTime);
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
