package me.sonam.authzmanager.tenant;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.util.Optional;

/**
 * Resolves the tenant authorization host and issuer URL from the current authzmanager request host.
 */
@Component("tenantAuthorizationUrlResolver")
public class TenantAuthorizationUrlResolver {
    private static final Logger LOG = LoggerFactory.getLogger(TenantAuthorizationUrlResolver.class);

    private final String hostLabel;
    private final URI fallbackIssuerUri;

    public TenantAuthorizationUrlResolver(
            @Value("${authzmanager.tenancy.host-label:admin}") String hostLabel,
            @Value("${issuerUri}") String issuerUri) {
        this.hostLabel = hostLabel;
        this.fallbackIssuerUri = URI.create(issuerUri);
    }

    /**
     * Converts an admin host like business1.admin.openissuer.com into business1.openissuer.com.
     */
    public String currentAuthorizationHost() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            LOG.info("no current request found; using fallback issuer host {}", fallbackIssuerUri.getHost());
            return fallbackIssuerUri.getHost();
        }
        String requestHost = currentRequestHost(request);
        LOG.info("resolve tenant authorization host from request host '{}'", requestHost);
        String expectedSegment = "." + hostLabel + ".";

        if (requestHost != null && requestHost.contains(expectedSegment)) {
            int index = requestHost.indexOf(expectedSegment);
            String tenantPrefix = requestHost.substring(0, index);
            String suffix = requestHost.substring(index + expectedSegment.length());
            String authorizationHost = tenantPrefix + "." + suffix;
            LOG.info("resolved tenant authorization host '{}' from admin host '{}'",
                    authorizationHost, requestHost);
            return authorizationHost;
        }

        if (requestHost != null && requestHost.equals(hostLabel)) {
            LOG.info("request host equals host label '{}'; using fallback issuer host {}",
                    hostLabel, fallbackIssuerUri.getHost());
            return fallbackIssuerUri.getHost();
        }

        LOG.info("request host '{}' did not match admin host label '{}'; using fallback issuer host {}",
                requestHost, hostLabel, fallbackIssuerUri.getHost());
        return fallbackIssuerUri.getHost();
    }

    /**
     * Builds the full issuer URI for the current tenant authorization host.
     */
    public String currentIssuerUri() {
        return toIssuerUri(currentAuthorizationHost());
    }

    /**
     * Adds forwarded headers so the internal authorization service resolves the correct tenant issuer.
     */
    public void applyTenantForwardHeaders(HttpHeaders headers) {
        headers.set("X-Forwarded-Host", currentAuthorizationHost());
        headers.set("X-Forwarded-Proto", issuerScheme());
        int port = issuerPort();
        if (port > 0) {
            headers.set("X-Forwarded-Port", String.valueOf(port));
        }
    }

    /**
     * Reconstructs an issuer URI using the configured fallback scheme and port with the tenant-specific host.
     */
    private String toIssuerUri(String authorizationHost) {
        int port = issuerPort();
        boolean defaultPort = ("http".equalsIgnoreCase(issuerScheme()) && port == 80)
                || ("https".equalsIgnoreCase(issuerScheme()) && port == 443)
                || port < 0;

        return defaultPort
                ? issuerScheme() + "://" + authorizationHost
                : issuerScheme() + "://" + authorizationHost + ":" + port;
    }

    /**
     * Returns the configured fallback issuer scheme.
     */
    private String issuerScheme() {
        return fallbackIssuerUri.getScheme();
    }

    /**
     * Returns the configured fallback issuer port.
     */
    private int issuerPort() {
        return fallbackIssuerUri.getPort();
    }

    /**
     * Returns the current servlet request so host-based tenant resolution can inspect the inbound host.
     */
    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getRequest();
    }

    private String currentRequestHost(HttpServletRequest request) {
        String requestHost = Optional.ofNullable(firstForwardedHost(request))
                .or(() -> Optional.ofNullable(request.getHeader(HttpHeaders.HOST)))
                .orElseGet(request::getServerName)
                .split(",")[0]
                .trim()
                .replaceFirst(":\\d+$", "");
        LOG.info("resolved raw request host '{}' from x-forwarded-host='{}', host='{}', serverName='{}'",
                requestHost, request.getHeader("X-Forwarded-Host"),
                request.getHeader(HttpHeaders.HOST), request.getServerName());
        return requestHost;
    }

    private String firstForwardedHost(HttpServletRequest request) {
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        if (forwardedHost != null && !forwardedHost.isBlank()) {
            return forwardedHost;
        }
        String forwarded = request.getHeader("Forwarded");
        if (forwarded == null || forwarded.isBlank()) {
            return null;
        }
        for (String segment : forwarded.split(";")) {
            String trimmed = segment.trim();
            if (trimmed.regionMatches(true, 0, "host=", 0, 5)) {
                return trimmed.substring(5).replace("\"", "");
            }
        }
        return null;
    }
}
