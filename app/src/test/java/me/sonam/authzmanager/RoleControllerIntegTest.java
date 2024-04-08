package me.sonam.authzmanager;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import me.sonam.authzmanager.controller.admin.organization.Organization;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.Resource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Application.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RoleControllerIntegTest {
    private static final Logger LOG = LoggerFactory.getLogger(RoleControllerIntegTest.class);
    private static MockWebServer mockWebServer;

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private WebClient webClient;
    private String messageClient = "messaging-client";

    private String clientSecret = "secret";
    private String base64ClientSecret = Base64.getEncoder().encodeToString(new StringBuilder(messageClient)
            .append(":").append(clientSecret).toString().getBytes());
    private UUID clientId = UUID.randomUUID();
    @Value("classpath:client-credential-access-token.json")
    private Resource refreshTokenResource;



    @LocalServerPort
    private int randomPort;
    @BeforeAll
    static void setupMockWebServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        LOG.info("host: {}, port: {}", mockWebServer.getHostName(), mockWebServer.getPort());
    }

    @AfterAll
    public static void shutdownMockWebServer() throws IOException {
        LOG.info("shutdown and close mockWebServer");
        mockWebServer.shutdown();
        mockWebServer.close();
    }

    private static String host;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) throws IOException {
        host = "http://localhost:"+ mockWebServer.getPort();
        r.add("auth-server.root", () -> host);
        r.add("organization-rest-service.root", () -> host);
    }

    @Test
    public void createOrganization() throws IOException {
        LOG.info("create organization with rest controller");


        signIn();

        webTestClient.post().uri("/admin/organizations").bodyValue(new Organization(null,
                "Red crown", UUID.randomUUID())).exchange().expectStatus().isOk().expectBody(String.class)
                .consumeWith(stringEntityExchangeResult -> LOG.info("result: {}", stringEntityExchangeResult.getResponseBody()));

    }

    private void signIn() throws IOException {
        final String jwtString = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJzb25hbSIsImlzcyI6InNvbmFtLmNsb3VkIiwiYXVkIjoic29uYW0uY2xvdWQiLCJqdGkiOiJmMTY2NjM1OS05YTViLTQ3NzMtOWUyNy00OGU0OTFlNDYzNGIifQ.KGFBUjghvcmNGDH0eM17S9pWkoLwbvDaDBGAx2AyB41yZ_8-WewTriR08JdjLskw1dsRYpMh9idxQ4BS6xmOCQ";

        final String jwtTokenMsg = " {\"access_token\":\"" + jwtString + "\"}";
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setResponseCode(200).setBody(jwtTokenMsg));

        //got response from auth-server authenticate call: {message=authentication success, roles=, userId=4d674e6e-408b-4423-8e4a-5611496facf0}

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setResponseCode(200).setBody("{\"message\": \"authentication success\", \"roles\": \"USER ADMIN\"," +
                        "\"userId\": \"4d674e6e-408b-4423-8e4a-5611496facf0\"}"));

        Page page = signIn(this.webClient.getPage("/login/login.html"), "sonam", "password");
        LOG.info("is html page: {}, url: {}, content: {}", page.isHtmlPage(), page.getUrl(), page.getWebResponse().getContentAsString());
    }
   // @Test
    public void createOrganization2() throws Exception {
        LOG.info("login to login/login.html");
        // Log in
        // this.webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        //set redirection false so we can login manually with code below
        //this.webClient.getOptions().setRedirectEnabled(false);

        final String jwtString = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJzb25hbSIsImlzcyI6InNvbmFtLmNsb3VkIiwiYXVkIjoic29uYW0uY2xvdWQiLCJqdGkiOiJmMTY2NjM1OS05YTViLTQ3NzMtOWUyNy00OGU0OTFlNDYzNGIifQ.KGFBUjghvcmNGDH0eM17S9pWkoLwbvDaDBGAx2AyB41yZ_8-WewTriR08JdjLskw1dsRYpMh9idxQ4BS6xmOCQ";

        final String jwtTokenMsg = " {\"access_token\":\"" + jwtString + "\"}";
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setResponseCode(200).setBody(jwtTokenMsg));

        //got response from auth-server authenticate call: {message=authentication success, roles=, userId=4d674e6e-408b-4423-8e4a-5611496facf0}

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setResponseCode(200).setBody("{\"message\": \"authentication success\", \"roles\": \"USER ADMIN\"," +
                        "\"userId\": \"4d674e6e-408b-4423-8e4a-5611496facf0\"}"));

        Page page = signIn(this.webClient.getPage("/login/login.html"), "sonam", "password");
        LOG.info("is html page: {}, url: {}, content: {}", page.isHtmlPage(), page.getUrl(), page.getWebResponse().getContentAsString());

        LOG.info("take first request");
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        LOG.info("should be acesstoken path for recordedRequest: {}", recordedRequest.getPath());
        AssertionsForClassTypes.assertThat(recordedRequest.getPath()).startsWith("/oauth2/token?grant_type=client_credentials");
        AssertionsForClassTypes.assertThat(recordedRequest.getMethod()).isEqualTo("POST");

        recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("PUT");
        assertThat(recordedRequest.getPath()).startsWith("/authenticate");

        LOG.info("assert that the page returned after login is admin/dashboard");
        assertThat(page.getUrl().toString()).isEqualTo("http://localhost:" + randomPort + "/admin/dashboard");

        this.webClient.getOptions().setJavaScriptEnabled(false);  //disable javascript to not load any bootstrap related js files
        LOG.info("get clientForm");
        Page organizationPage = this.webClient.getPage("/admin/organizations/form");
        LOG.info("submit to ceate client");

        Organization organization = new Organization(null, "White Buffalo Org", null);
        fillInForm((HtmlPage) organizationPage, organization);

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setResponseCode(200).setBody(jwtTokenMsg));
        recordedRequest = mockWebServer.takeRequest();
        LOG.info("should be acesstoken path for recordedRequest: {}", recordedRequest.getPath());
        AssertionsForClassTypes.assertThat(recordedRequest.getPath()).startsWith("/oauth2/token?grant_type=client_credentials");
        AssertionsForClassTypes.assertThat(recordedRequest.getMethod()).isEqualTo("POST");

        final String organizationResponse = "{\"id\": \"3bfd70e3-242e-40c9-ae3d-fa88864919b5\"}";
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setResponseCode(200).setBody(organizationResponse));

        LOG.info("take the organization creation request");
        recordedRequest = mockWebServer.takeRequest();
        LOG.info("should be acesstoken path for recordedRequest: {}", recordedRequest.getPath());
        AssertionsForClassTypes.assertThat(recordedRequest.getPath()).startsWith("/organizations");
        AssertionsForClassTypes.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        LOG.info("asserted the path and Http method of POST");
    }

    public void getOrganizations() throws IOException {
        LOG.info("get organizations");

        Page organizationPage = this.webClient.getPage("/admin/organizations");
    }

    private static <P extends Page> P fillInForm(HtmlPage page, Organization organization) throws IOException {
        HtmlInput organizationId = page.querySelector("input[name=\"name\"]");
        organizationId.type(organization.getName());
        LOG.info("set name: {}", organization.getName());

        HtmlButton submit = page.querySelector("button");
        LOG.info("sending form to create organization");
        return submit.click();
    }

    private static <P extends Page> P signIn(HtmlPage page, String username, String password) throws IOException {
        //LOG.info("page: {}, done end", page.toString());
        HtmlInput usernameInput = page.querySelector("input[name=\"username\"]");
        HtmlInput passwordInput = page.querySelector("input[name=\"password\"]");
        HtmlButton signInButton = page.querySelector("button");

        usernameInput.type(username);
        passwordInput.type(password);
        return signInButton.click();
    }
}
