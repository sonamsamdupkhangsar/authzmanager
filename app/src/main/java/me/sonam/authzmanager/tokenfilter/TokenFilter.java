package me.sonam.authzmanager.tokenfilter;

import com.nimbusds.oauth2.sdk.auth.JWTAuthentication;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import io.kubernetes.client.util.credentials.UsernamePasswordAuthentication;
import org.apache.http.auth.AUTH;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final TokenService tokenService;

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
            if (request.headers().getFirst(AUTHORIZATION) != null) {
                LOG.info("request contains authorization header already");
                LOG.debug("authorization header: {}", request.headers().getFirst(AUTHORIZATION));

                if (request.headers().getFirst(AUTHORIZATION).contains("Bearer null")) {
                    LOG.warn("request header contains Bearer but null token");
                    return processTokenFilter(null, request, next);
                }
                ClientRequest filtered = ClientRequest.from(request).build();
                return next.exchange(filtered);
            }
            else {
                LOG.info("request does not contain authorization header");
                return processTokenFilter(null, request, next);
            }
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
                    return getAccessTokenCheck(accessToken)
                            .flatMap(jwtAccessToken -> {
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

    private boolean isExpired(LocalDateTime tokenTime) {
        LocalDateTime tokenExpiredTime = LocalDateTime.now().minus(Duration.ofSeconds(tokenExpireSeconds));

        return tokenTime.isBefore(tokenExpiredTime);
    }

    private Mono<String> getAccessTokenCheck(TokenRequestFilter.RequestFilter.AccessToken accessToken) {
        if (accessToken.getAccessToken() != null && !isExpired(accessToken.getAccessTokenCreationTime())) {
            LOG.info("access token is not expired, return that instead");
            return Mono.just(accessToken.getAccessToken());
        }

        return getAccessToken(accessToken);
    }

    private Mono<String> getAccessToken(TokenRequestFilter.RequestFilter.AccessToken accessToken) {
        LOG.info("making a access-token request to endpoint: {}",oauth2TokenEndpoint);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("grant_type", grantType);

        List<String> scopeList = Arrays.stream(accessToken.getScopes().split(" ")).toList();
        body.add("scopes", scopeList);

        LOG.debug("add body payload for grant type and scopes");

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().post().uri(oauth2TokenEndpoint)
                .bodyValue(body)
                .headers(httpHeaders -> httpHeaders.setBasicAuth(accessToken.getBase64EncodedClientIdSecret()))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve();

        return responseSpec.bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {}).map(map -> {
            LOG.debug("response for '{}' is in map: {}", oauth2TokenEndpoint, map);
            if (map.get("access_token") != null) {
                accessToken.setAccessToken(map.get("access_token"));
                return map.get("access_token");
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
