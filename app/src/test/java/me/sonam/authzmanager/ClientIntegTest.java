package me.sonam.authzmanager;


import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import me.sonam.authzmanager.controller.admin.oauth2.ClientAuthenticationMethod;
import me.sonam.authzmanager.controller.admin.oauth2.OauthClient;
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

import java.io.IOException;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Application.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ClientIntegTest {
    private static final Logger LOG = LoggerFactory.getLogger(ClientIntegTest.class);

    private static MockWebServer mockWebServer;

    @Autowired
    private WebClient webTestClient;

    private String messageClient = "messaging-client";

    private String clientSecret = "secret";
    private String base64ClientSecret = Base64.getEncoder().encodeToString(new StringBuilder(messageClient)
            .append(":").append(clientSecret).toString().getBytes());
    private UUID clientId = UUID.randomUUID();
    @Value("classpath:client-credential-access-token.json")
    private Resource refreshTokenResource;



    @Autowired
    private WebClient webClient;
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
    }

    @Test
    public void clientCreate() throws Exception {
        LOG.info("login to login/login.html");
        // Log in
       // this.webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        //set redirection false so we can login manually with code below
        //this.webClient.getOptions().setRedirectEnabled(false);

        final String jwtString= "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJzb25hbSIsImlzcyI6InNvbmFtLmNsb3VkIiwiYXVkIjoic29uYW0uY2xvdWQiLCJqdGkiOiJmMTY2NjM1OS05YTViLTQ3NzMtOWUyNy00OGU0OTFlNDYzNGIifQ.KGFBUjghvcmNGDH0eM17S9pWkoLwbvDaDBGAx2AyB41yZ_8-WewTriR08JdjLskw1dsRYpMh9idxQ4BS6xmOCQ";

        final String jwtTokenMsg = " {\"access_token\":\""+jwtString+"\"}";
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setResponseCode(200).setBody(jwtTokenMsg));

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setResponseCode(200).setBody("{\"roles\": \"USER ADMIN\"}"));

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
        assertThat(page.getUrl().toString()).isEqualTo("http://localhost:"+randomPort+"/admin/dashboard");

        this.webClient.getOptions().setJavaScriptEnabled(false);  //disable javascript to not load any bootstrap related js files
        LOG.info("get clientForm");
        Page clientPage = this.webClient.getPage("/admin/clients/createForm");
        LOG.info("submit to ceate client");
        OauthClient client = new OauthClient();
        client.setClientId("");
        client.setClientIdIssuedAt("");
        client.setClientSecret("");
        client.setClientSecretExpiresAt("");
        client.setClientName("");
        client.setClientAuthenticationMethods(new ArrayList<>());
        client.setAuthorizationGrantTypes(new ArrayList<>());
        client.setRedirectUris("");
        client.setPostLogoutRedirectUris("");

        client.setClientSettings("");
        client.setTokenSettings("");

        fillInForm((HtmlPage) clientPage, client);
    }

    private static <P extends Page> P fillInForm(HtmlPage page, OauthClient client) throws IOException {
        HtmlInput clientId = page.querySelector("input[name=\"clientId\"]");
        HtmlInput clientIssuedAt = page.querySelector("input[name=\"clientIdIssuedAt\"]");
        HtmlInput clientSecret = page.querySelector("input[name=\"clientSecret\"]");
        HtmlInput clientSecretExpiresAt = page.querySelector("input[name=\"clientSecretExpiresAt\"]");
        HtmlInput clientName = page.querySelector("input[name=\"clientName\"]");
        HtmlCheckBoxInput clientAuthenticationMethods = page.querySelector("input[name=\"clientAuthenticationMethods\"]");
        HtmlCheckBoxInput authorizationGrantTypes = page.querySelector("input[name=\"authorizationGrantTypes\"]");

        HtmlInput redirectUris = page.querySelector("input[name=\"redirectUris\"]");
        HtmlInput postLogoutRedirectUris = page.querySelector("input[name=\"postLogoutRedirectUris\"]");
        HtmlCheckBoxInput scopes = page.querySelector("input[name=\"scopes\"]");
        HtmlInput clientSettings = page.querySelector("input[name=\"clientSettings\"]");
        HtmlInput tokenSettings = page.querySelector("input[name=\"tokenSettings\"]");

        clientId.type(client.getClientId());
        clientIssuedAt.type(client.getClientIdIssuedAt().toString());
        clientSecret.type(client.getClientSecret());
        clientSecretExpiresAt.type(client.getClientSecretExpiresAt().toString());
        clientName.type(client.getClientName());

        clientAuthenticationMethods.setAttribute("checked", "checked");

        authorizationGrantTypes.setAttribute("checked", "checked");
        redirectUris.type(client.getRedirectUris().toString());
        postLogoutRedirectUris.type(client.getPostLogoutRedirectUris().toString());
        scopes.setAttribute("checked", "checked");

        clientSettings.type(client.getClientSettings().toString());
        tokenSettings.type(client.getTokenSettings().toString());

        HtmlButton submit = page.querySelector("button");
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
