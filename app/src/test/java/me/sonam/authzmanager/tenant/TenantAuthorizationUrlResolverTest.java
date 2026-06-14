package me.sonam.authzmanager.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies tenant host and issuer URL resolution from authzmanager request hosts.
 */
class TenantAuthorizationUrlResolverTest {
    /**
     * Clears request state between tests so host resolution does not leak across test cases.
     */
    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * Verifies that the admin label is removed to produce the tenant authorization host.
     */
    @Test
    void resolvesAuthorizationHostFromAdminSubdomain() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("business1.admin.openissuer.com");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        TenantAuthorizationUrlResolver resolver =
                new TenantAuthorizationUrlResolver("admin", "https://platform.openissuer.com");

        assertEquals("business1.openissuer.com", resolver.currentAuthorizationHost());
        assertEquals("https://business1.openissuer.com", resolver.currentIssuerUri());
    }

    /**
     * Verifies that local issuer ports are preserved when constructing tenant-specific issuer URLs.
     */
    @Test
    void preservesConfiguredIssuerPortForLocalDevelopment() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("business2.admin.openissuer.test");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        TenantAuthorizationUrlResolver resolver =
                new TenantAuthorizationUrlResolver("admin", "http://platform.openissuer.test:9001");

        assertEquals("business2.openissuer.test", resolver.currentAuthorizationHost());
        assertEquals("http://business2.openissuer.test:9001", resolver.currentIssuerUri());
    }

    @Test
    void resolvesFreeAuthorizationHostFromLocalAdminSubdomain() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("free.admin.openissuer.test");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        TenantAuthorizationUrlResolver resolver =
                new TenantAuthorizationUrlResolver("admin", "http://platform.openissuer.test:9001");

        assertEquals("free.openissuer.test", resolver.currentAuthorizationHost());
        assertEquals("http://free.openissuer.test:9001", resolver.currentIssuerUri());
    }

    @Test
    void resolvesAuthorizationHostFromForwardedHostBeforeServerName() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("10.0.0.244");
        request.addHeader("X-Forwarded-Host", "free.admin.openissuer.test:9093");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        TenantAuthorizationUrlResolver resolver =
                new TenantAuthorizationUrlResolver("admin", "http://platform.openissuer.test:9001");

        assertEquals("free.openissuer.test", resolver.currentAuthorizationHost());
        assertEquals("http://free.openissuer.test:9001", resolver.currentIssuerUri());
    }

    @Test
    void resolvesAuthorizationHostFromForwardedHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("localhost");
        request.addHeader("Forwarded", "proto=http;host=business2.admin.openissuer.test:9093");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        TenantAuthorizationUrlResolver resolver =
                new TenantAuthorizationUrlResolver("admin", "http://platform.openissuer.test:9001");

        assertEquals("business2.openissuer.test", resolver.currentAuthorizationHost());
        assertEquals("http://business2.openissuer.test:9001", resolver.currentIssuerUri());
    }
}
