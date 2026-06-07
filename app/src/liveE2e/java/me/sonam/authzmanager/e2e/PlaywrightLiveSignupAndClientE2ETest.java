package me.sonam.authzmanager.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opt-in live E2E test that records browser screenshots and videos.
 *
 * Required environment:
 * AUTHZMGR_E2E_BASE_URLS=http://business1.admin.openissuer.test:8080,http://business2.admin.openissuer.test:8080
 * AUTHZMGR_E2E_USERNAME=admin@example.com
 * AUTHZMGR_E2E_PASSWORD=...
 *
 * Optional environment:
 * AUTHZMGR_E2E_USERS_PER_SUBDOMAIN=1
 * AUTHZMGR_E2E_CLIENTS_PER_SUBDOMAIN=1
 * AUTHZMGR_E2E_USER_EMAIL_TEMPLATE=sonamhava+{auth_id}@gmail.com
 * AUTHZMGR_E2E_WAIT_FOR_ACTIVATION=false
 * AUTHZMGR_E2E_ACTIVATION_WAIT_SECONDS=300
 * AUTHZMGR_E2E_CLIENT_SECRET=live-e2e-secret-123
 * AUTHZMGR_E2E_REDIRECT_URI=https://example.com/callback
 * AUTHZMGR_E2E_HEADLESS=true
 * AUTHZMGR_E2E_RECORD_VIDEO=true
 * AUTHZMGR_E2E_SCREENSHOTS=true
 * AUTHZMGR_E2E_ARTIFACT_DIR=build/live-e2e-artifacts
 */
class PlaywrightLiveSignupAndClientE2ETest {
    private static final String BASE_URLS = requireEnv("AUTHZMGR_E2E_BASE_URLS");
    private static final String USERNAME = requireEnv("AUTHZMGR_E2E_USERNAME");
    private static final String PASSWORD = requireEnv("AUTHZMGR_E2E_PASSWORD");
    private static final int USERS_PER_SUBDOMAIN = intEnv("AUTHZMGR_E2E_USERS_PER_SUBDOMAIN", 1);
    private static final int CLIENTS_PER_SUBDOMAIN = intEnv("AUTHZMGR_E2E_CLIENTS_PER_SUBDOMAIN", 1);
    private static final String USER_EMAIL_TEMPLATE = env("AUTHZMGR_E2E_USER_EMAIL_TEMPLATE",
            "live-e2e+{auth_id}@example.com");
    private static final boolean WAIT_FOR_ACTIVATION = boolEnv("AUTHZMGR_E2E_WAIT_FOR_ACTIVATION", false);
    private static final int ACTIVATION_WAIT_SECONDS = intEnv("AUTHZMGR_E2E_ACTIVATION_WAIT_SECONDS", 300);
    private static final String CLIENT_SECRET = env("AUTHZMGR_E2E_CLIENT_SECRET", "live-e2e-secret-123");
    private static final String REDIRECT_URI = env("AUTHZMGR_E2E_REDIRECT_URI", "https://example.com/callback");
    private static final boolean HEADLESS = boolEnv("AUTHZMGR_E2E_HEADLESS", true);
    private static final boolean RECORD_VIDEO = boolEnv("AUTHZMGR_E2E_RECORD_VIDEO", true);
    private static final boolean SCREENSHOTS = boolEnv("AUTHZMGR_E2E_SCREENSHOTS", true);
    private static final Path ARTIFACT_DIR = Path.of(env("AUTHZMGR_E2E_ARTIFACT_DIR", "build/live-e2e-artifacts"));

    @Test
    void signsUpUsersAndCreatesClientsWithScreenshotsAndVideo() throws Exception {
        List<String> baseUrls = Arrays.stream(BASE_URLS.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        assertFalse(baseUrls.isEmpty(), "AUTHZMGR_E2E_BASE_URLS must contain at least one URL");
        Files.createDirectories(ARTIFACT_DIR);

        String runId = Long.toString(Instant.now().toEpochMilli());

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(HEADLESS));

            for (String baseUrl : baseUrls) {
                String normalizedBaseUrl = trimTrailingSlash(baseUrl);
                String subdomainSlug = slug(normalizedBaseUrl);
                Path subdomainDir = ARTIFACT_DIR.resolve(subdomainSlug + "-" + runId);
                Files.createDirectories(subdomainDir);
                Path pendingActivations = subdomainDir.resolve("pending-activations.txt");

                Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                        .setViewportSize(1440, 1000);
                if (RECORD_VIDEO) {
                    contextOptions.setRecordVideoDir(subdomainDir.resolve("videos"))
                            .setRecordVideoSize(1440, 1000);
                }

                BrowserContext context = browser.newContext(contextOptions);
                Page page = context.newPage();

                try {
                    openAuthenticated(page, normalizedBaseUrl + "/admin/dashboard");
                    screenshot(page, subdomainDir, "01-dashboard");

                    for (int i = 1; i <= USERS_PER_SUBDOMAIN; i++) {
                        String authId = "live-e2e-" + subdomainSlug + "-" + runId + "-u" + i;
                        String email = emailForAuthId(authId);
                        signUpUser(page, normalizedBaseUrl, subdomainDir, authId, email, i);
                        appendPendingActivation(pendingActivations, normalizedBaseUrl, authId, email);
                    }

                    waitForActivationIfEnabled(pendingActivations);

                    for (int i = 1; i <= CLIENTS_PER_SUBDOMAIN; i++) {
                        String clientId = "live-e2e-" + subdomainSlug + "-" + runId + "-c" + i;
                        createClient(page, normalizedBaseUrl, subdomainDir, clientId, i);
                    }
                }
                finally {
                    context.close();
                }
            }

            browser.close();
        }
    }

    private void signUpUser(Page page, String baseUrl, Path artifactDir, String authId, String email, int index) {
        openAuthenticated(page, baseUrl + "/admin/organizations/users");
        screenshot(page, artifactDir, "02-signup-form-" + index);

        page.locator("#firstName").fill("Live");
        page.locator("#lastName").fill("E2E");
        page.locator("#email").fill(email);
        page.locator("#authenticationId").fill(authId);

        Locator setPassword = page.locator("#setPassword");
        if (setPassword.count() > 0 && !setPassword.isChecked()) {
            setPassword.check();
        }
        page.locator("#password").fill("Password123!");

        Locator active = page.locator("#active");
        if (active.count() > 0 && active.isChecked()) {
            active.uncheck();
        }

        page.locator("#submit").click();
        page.waitForLoadState();
        screenshot(page, artifactDir, "03-signup-success-" + index);
        assertTrue(page.locator("body").innerText().contains("User Signup Success"),
                "Expected signup success for " + email + ". Page text: " + page.locator("body").innerText());
    }

    private void createClient(Page page, String baseUrl, Path artifactDir, String clientId, int index) {
        openAuthenticated(page, baseUrl + "/admin/clients/createForm");
        screenshot(page, artifactDir, "04-client-form-" + index);

        fillIfPresent(page, "#clientId", clientId);
        fillIfPresent(page, "#clientSecret", CLIENT_SECRET);
        fillIfPresent(page, "#clientName", clientId);
        fillIfPresent(page, "#redirectUris", REDIRECT_URI);
        fillIfPresent(page, "#postLogoutRedirectUris", REDIRECT_URI);
        fillIfPresent(page, "input[name='customScopes']", "message.read,message.write");

        checkByNameAndValue(page, "clientAuthenticationMethods", "CLIENT_SECRET_BASIC");
        checkByNameAndValue(page, "authorizationGrantTypes", "CLIENT_CREDENTIALS");
        checkByNameAndValue(page, "authorizationGrantTypes", "AUTHORIZATION_CODE");
        checkByNameAndValue(page, "scopes", "OPENID");
        checkByNameAndValue(page, "scopes", "PROFILE");

        page.locator("form button[type='submit']").first().click();
        page.waitForLoadState();
        screenshot(page, artifactDir, "05-client-success-" + index);
        assertTrue(page.locator("body").innerText().contains("Client created successfully"),
                "Expected client create success for " + clientId + ". Page text: " + page.locator("body").innerText());
    }

    private void openAuthenticated(Page page, String url) {
        page.navigate(url);
        page.waitForLoadState();
        loginIfNeeded(page);
    }

    private void loginIfNeeded(Page page) {
        Locator username = page.locator("input[name='username']");
        Locator password = page.locator("input[name='password']");
        if (username.count() == 0 || password.count() == 0) {
            return;
        }

        username.first().fill(USERNAME);
        password.first().fill(PASSWORD);
        page.locator("button[type='submit'], input[type='submit']").first().click();
        page.waitForLoadState();

        String body = page.locator("body").innerText().toLowerCase();
        if (body.contains("authorize") || body.contains("consent")) {
            Locator submit = page.locator("button[type='submit'], input[type='submit']");
            if (submit.count() > 0) {
                submit.first().click();
                page.waitForLoadState();
            }
        }
    }

    private void fillIfPresent(Page page, String selector, String value) {
        Locator locator = page.locator(selector);
        if (locator.count() > 0) {
            locator.first().fill(value);
        }
    }

    private void checkByNameAndValue(Page page, String name, String value) {
        Locator checkbox = page.locator("input[name='" + name + "'][value='" + value + "']");
        if (checkbox.count() == 0) {
            throw new IllegalStateException("Checkbox not found: " + name + "=" + value);
        }
        checkbox.first().check();
    }

    private void screenshot(Page page, Path artifactDir, String name) {
        if (!SCREENSHOTS) {
            return;
        }
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(artifactDir.resolve(name + ".png"))
                .setFullPage(true));
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

    private static boolean boolEnv(String name, boolean defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : Boolean.parseBoolean(value);
    }

    private static String emailForAuthId(String authId) {
        return USER_EMAIL_TEMPLATE.replace("{auth_id}", authId);
    }

    private static void appendPendingActivation(Path file, String baseUrl, String authId, String email)
            throws java.io.IOException {
        String line = "baseUrl=" + baseUrl + ", authId=" + authId + ", email=" + email + System.lineSeparator();
        Files.writeString(file, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static void waitForActivationIfEnabled(Path pendingActivations) throws InterruptedException {
        if (!WAIT_FOR_ACTIVATION) {
            return;
        }

        System.out.println("Waiting " + ACTIVATION_WAIT_SECONDS + " seconds for manual user activation.");
        System.out.println("Pending activations: " + pendingActivations.toAbsolutePath());
        Thread.sleep(ACTIVATION_WAIT_SECONDS * 1000L);
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
