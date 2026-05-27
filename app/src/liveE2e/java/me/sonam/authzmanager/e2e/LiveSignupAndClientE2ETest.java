package me.sonam.authzmanager.e2e;

import org.htmlunit.BrowserVersion;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opt-in live browser-style E2E test.
 *
 * Required environment:
 * AUTHZMGR_E2E_BASE_URLS=https://business1.admin.openissuer.com,https://business2.admin.openissuer.com
 * AUTHZMGR_E2E_USERNAME=admin@example.com
 * AUTHZMGR_E2E_PASSWORD=...
 *
 * Optional environment:
 * AUTHZMGR_E2E_USERS_PER_SUBDOMAIN=1
 * AUTHZMGR_E2E_CLIENTS_PER_SUBDOMAIN=1
 * AUTHZMGR_E2E_USER_EMAIL_DOMAIN=example.com
 * AUTHZMGR_E2E_CLIENT_SECRET=live-e2e-secret-123
 * AUTHZMGR_E2E_REDIRECT_URI=https://example.com/callback
 */
class LiveSignupAndClientE2ETest {
    private static final String BASE_URLS = requireEnv("AUTHZMGR_E2E_BASE_URLS");
    private static final String USERNAME = requireEnv("AUTHZMGR_E2E_USERNAME");
    private static final String PASSWORD = requireEnv("AUTHZMGR_E2E_PASSWORD");
    private static final int USERS_PER_SUBDOMAIN = intEnv("AUTHZMGR_E2E_USERS_PER_SUBDOMAIN", 1);
    private static final int CLIENTS_PER_SUBDOMAIN = intEnv("AUTHZMGR_E2E_CLIENTS_PER_SUBDOMAIN", 1);
    private static final String USER_EMAIL_DOMAIN = env("AUTHZMGR_E2E_USER_EMAIL_DOMAIN", "example.com");
    private static final String CLIENT_SECRET = env("AUTHZMGR_E2E_CLIENT_SECRET", "live-e2e-secret-123");
    private static final String REDIRECT_URI = env("AUTHZMGR_E2E_REDIRECT_URI", "https://example.com/callback");

    @Test
    void signsUpUsersAndCreatesClientsInEachSubdomain() throws Exception {
        List<String> baseUrls = Arrays.stream(BASE_URLS.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        assertFalse(baseUrls.isEmpty(), "AUTHZMGR_E2E_BASE_URLS must contain at least one URL");

        String runId = Long.toString(Instant.now().toEpochMilli());

        try (WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setRedirectEnabled(true);

            for (String baseUrl : baseUrls) {
                String normalizedBaseUrl = trimTrailingSlash(baseUrl);
                ensureLoggedIn(webClient, normalizedBaseUrl);

                for (int i = 1; i <= USERS_PER_SUBDOMAIN; i++) {
                    String email = "live-e2e-" + slug(normalizedBaseUrl) + "-" + runId + "-u" + i
                            + "@" + USER_EMAIL_DOMAIN;
                    HtmlPage signupResult = signUpUser(webClient, normalizedBaseUrl, email);
                    assertContains(signupResult, "User Signup Success", "signup result for " + email);
                }

                for (int i = 1; i <= CLIENTS_PER_SUBDOMAIN; i++) {
                    String clientId = "live-e2e-" + slug(normalizedBaseUrl) + "-" + runId + "-c" + i;
                    HtmlPage clientResult = createClient(webClient, normalizedBaseUrl, clientId);
                    assertContains(clientResult, "Client created successfully", "client create result for " + clientId);
                }
            }
        }
    }

    private HtmlPage signUpUser(WebClient webClient, String baseUrl, String email) throws IOException {
        HtmlPage page = openAuthenticated(webClient, baseUrl + "/admin/organizations/users");
        HtmlForm form = firstForm(page);

        setInput(form, "firstName", "Live");
        setInput(form, "lastName", "E2E");
        setInput(form, "email", email);
        setInput(form, "authenticationId", email);

        HtmlInput setPassword = optionalInputById(page, "setPassword");
        if (setPassword != null && !setPassword.isChecked()) {
            setPassword.setChecked(true);
        }
        setInput(form, "password", "Password123!");

        HtmlInput active = optionalInputByName(form, "active");
        if (active != null) {
            active.setChecked(false);
        }

        return submit(form);
    }

    private HtmlPage createClient(WebClient webClient, String baseUrl, String clientId) throws IOException {
        HtmlPage page = openAuthenticated(webClient, baseUrl + "/admin/clients/createForm");
        HtmlForm form = firstForm(page);

        setInput(form, "clientId", clientId);
        setInputIfPresent(form, "clientSecret", CLIENT_SECRET);
        setInputIfPresent(form, "clientName", clientId);
        setInputIfPresent(form, "redirectUris", REDIRECT_URI);
        setInputIfPresent(form, "postLogoutRedirectUris", REDIRECT_URI);
        setInputIfPresent(form, "customScopes", "message.read,message.write");

        checkByNameAndValue(page, "clientAuthenticationMethods", "CLIENT_SECRET_BASIC");
        checkByNameAndValue(page, "authorizationGrantTypes", "CLIENT_CREDENTIALS");
        checkByNameAndValue(page, "authorizationGrantTypes", "AUTHORIZATION_CODE");
        checkByNameAndValue(page, "scopes", "OPENID");
        checkByNameAndValue(page, "scopes", "PROFILE");

        return submit(form);
    }

    private HtmlPage openAuthenticated(WebClient webClient, String url) throws IOException {
        HtmlPage page = webClient.getPage(url);
        return loginIfNeeded(page);
    }

    private void ensureLoggedIn(WebClient webClient, String baseUrl) throws IOException {
        openAuthenticated(webClient, baseUrl + "/admin/dashboard");
    }

    private HtmlPage loginIfNeeded(HtmlPage page) throws IOException {
        HtmlInput usernameInput = optionalInputByName(page, "username");
        HtmlInput passwordInput = optionalInputByName(page, "password");
        if (usernameInput == null || passwordInput == null) {
            return page;
        }

        usernameInput.setValueAttribute(USERNAME);
        passwordInput.setValueAttribute(PASSWORD);
        HtmlPage nextPage = clickFirstSubmit(page);
        return continueAuthorizationIfNeeded(nextPage);
    }

    private HtmlPage continueAuthorizationIfNeeded(HtmlPage page) throws IOException {
        String text = page.asNormalizedText().toLowerCase();
        if (!text.contains("authorize") && !text.contains("consent")) {
            return page;
        }

        List<HtmlElement> buttons = page.getByXPath("//button[@type='submit']|//input[@type='submit']");
        if (buttons.isEmpty()) {
            return page;
        }
        return buttons.getFirst().click();
    }

    private HtmlPage clickFirstSubmit(HtmlPage page) throws IOException {
        List<HtmlElement> submits = page.getByXPath("//button[@type='submit']|//input[@type='submit']");
        if (submits.isEmpty()) {
            throw new IllegalStateException("No submit button found on " + page.getUrl());
        }
        return submits.getFirst().click();
    }

    private HtmlForm firstForm(HtmlPage page) {
        if (page.getForms().isEmpty()) {
            throw new IllegalStateException("No form found on " + page.getUrl() + ". Page text: "
                    + page.asNormalizedText());
        }
        return page.getForms().getFirst();
    }

    private HtmlPage submit(HtmlForm form) throws IOException {
        HtmlButton button = form.getFirstByXPath(".//button[@type='submit']");
        if (button != null) {
            return button.click();
        }

        HtmlInput submit = form.getFirstByXPath(".//input[@type='submit']");
        if (submit != null) {
            return submit.click();
        }

        throw new IllegalStateException("No submit control found in form on " + form.getPage().getUrl());
    }

    private void setInput(HtmlForm form, String name, String value) {
        HtmlInput input = form.getInputByName(name);
        input.setValueAttribute(value);
    }

    private void setInputIfPresent(HtmlForm form, String name, String value) {
        HtmlInput input = optionalInputByName(form, name);
        if (input != null) {
            input.setValueAttribute(value);
        }
    }

    private HtmlInput optionalInputByName(HtmlPage page, String name) {
        return page.getFirstByXPath("//input[@name='" + name + "']");
    }

    private HtmlInput optionalInputByName(HtmlForm form, String name) {
        return form.getFirstByXPath(".//input[@name='" + name + "']");
    }

    private HtmlInput optionalInputById(HtmlPage page, String id) {
        return page.getFirstByXPath("//input[@id='" + id + "']");
    }

    private void checkByNameAndValue(HtmlPage page, String name, String value) {
        HtmlInput checkbox = page.getFirstByXPath("//input[@name='" + name + "' and @value='" + value + "']");
        if (checkbox == null) {
            throw new IllegalStateException("Checkbox not found: " + name + "=" + value + " on " + page.getUrl());
        }
        checkbox.setChecked(true);
    }

    private void assertContains(HtmlPage page, String expected, String context) {
        assertTrue(page.asNormalizedText().contains(expected),
                () -> "Expected " + context + " to contain '" + expected + "' on " + page.getUrl()
                        + ". Page text: " + page.asNormalizedText());
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required for liveE2eTest");
        }
        return value;
    }

    private static int intEnv(String name, int defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value);
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String slug(String value) {
        return value.replaceFirst("^https?://", "")
                .replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("(^-|-$)", "")
                .toLowerCase();
    }
}
