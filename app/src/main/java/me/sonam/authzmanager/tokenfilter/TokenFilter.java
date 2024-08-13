package me.sonam.authzmanager.tokenfilter;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
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

    private final String oauth2TokenEndpoint;

    private final String grantType;
    private final String accessTokenPath;
    private final TokenRequestFilter tokenRequestFilter;

    private final WebClient.Builder webClientBuilder;


    public TokenFilter(WebClient.Builder webClientBuilder, TokenRequestFilter tokenRequestFilter, String oauth2TokenEndpoint, String grantType, String accessTokenPath) {
        this.webClientBuilder = webClientBuilder;
        this.grantType = grantType;
        this.tokenRequestFilter = tokenRequestFilter;
        this.oauth2TokenEndpoint = oauth2TokenEndpoint;
        this.accessTokenPath = accessTokenPath;
    }

    public ExchangeFilterFunction renewTokenFilter() {
        return (request, next) -> {

            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            String path;

            if (requestAttributes instanceof ServletRequestAttributes) {
                HttpServletRequest servletRequest = ((ServletRequestAttributes)requestAttributes).getRequest();
                LOG.debug("is a server request:  {}, contextPath: {}", servletRequest.getPathInfo(), servletRequest.getContextPath());
                if (servletRequest.getPathInfo() != null) {
                    path = servletRequest.getPathInfo();
                    LOG.info("inPath: {}", path);
                }
                else {
                    LOG.info("serverRequest.pathInfo is null");
                    path = "";
                }
            }
            else {
                path = "";
            }

            LOG.info("inbound path: {}, outbound request path: {}", path, request.url().getPath());
            if (request.url().getPath().equals(accessTokenPath)) {
                LOG.debug("no need to request access token when going to that path: {}", request.url().getPath());
                ClientRequest clientRequest = ClientRequest.from(request).build();
                return next.exchange(clientRequest);
            }
            else {
                LOG.debug("going thru request filters") ;
                int index = 0;
                for (TokenRequestFilter.RequestFilter requestFilter : tokenRequestFilter.getRequestFilters()) {
                    LOG.info("checking requestFilter[{}]  {}", index++, requestFilter);

                    if (!requestFilter.getInHttpMethodSet().isEmpty()) {

                        LOG.debug("httpMethods: {} provided, actual inbound httpMethod: {}", requestFilter.getInHttpMethodSet(),
                                request.method().name());
                        LOG.info("requestFilter.getInHttpMethodSet().contains(request.method().name().toLowerCase(): {}", requestFilter.getInHttpMethodSet().contains(request.method().name().toLowerCase()));
                        if (requestFilter.getInHttpMethodSet().contains(request.method().name().toLowerCase())) {
                            LOG.info("outbound request method {} matched with provided httpMethod", request.method().name());

                            boolean matchInPath = requestFilter.getInSet().stream().anyMatch(w -> {
                                boolean matched = path.matches(w);
                                if (LOG.isDebugEnabled() && matched) {
                                    LOG.debug("inPath {} matched with regEx {}", path, w);
                                }
                                return matched;
                            });

                            if (matchInPath) {
                                LOG.info("inPath {} match found, check outPath next", path);
                                boolean matchOutPath = requestFilter.getOutSet().stream().anyMatch(w -> {
                                    boolean value = request.url().getPath().matches(w);
                                    LOG.debug("request path {}, regex expression '{}' matches? : {}", request.url().getPath(), w, value);
                                    return value;
                                });
                                if (matchOutPath) {
                                    LOG.info("inbound and outbound path matched");
                                    return getClientRequest(request, next, requestFilter);
                                } else {
                                    LOG.info("no match found for outbound path {} ",
                                            request.url().getPath());
                                }
                            }
                            else {
                                LOG.info("no match found for inbound path {}", path);
                            }
                        }
                    }
                    else {
                        LOG.info("inHttpMethodSet is empty");
                    }
                }

                LOG.info("no match found");
                ClientRequest filtered = ClientRequest.from(request)
                        .build();
                return next.exchange(filtered);
            }
        };
    }

    private Mono<ClientResponse> getClientRequest(ClientRequest request, ExchangeFunction next, TokenRequestFilter.RequestFilter jwt) {
        LOG.info("getClientRequest()");
        if (jwt.getAccessToken().getOption().equals(TokenRequestFilter.RequestFilter.AccessToken.JwtOption.forward)) {
            LOG.info("forwarding with authorization header if present is {}", request.headers().get(HttpHeaders.AUTHORIZATION));
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
