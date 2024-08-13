package me.sonam.authzmanager;


import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import me.sonam.authzmanager.oauth2.OauthClient;
import okhttp3.mockwebserver.MockWebServer;
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
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Application.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ClientPageTest {
    private static final Logger LOG = LoggerFactory.getLogger(ClientPageTest.class);

    private static MockWebServer mockWebServer;

    //@Autowired
    //private WebClient webTestClient;

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
        LOG.info("login to login/authlogin.html");
/*
        // Log in
       // this.webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        //set redirection false so we can login manually with code below
        //this.webClient.getOptions().setRedirectEnabled(false);

        final String jwtString= "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJzb25hbSIsImlzcyI6InNvbmFtLmNsb3VkIiwiYXVkIjoic29uYW0uY2xvdWQiLCJqdGkiOiJmMTY2NjM1OS05YTViLTQ3NzMtOWUyNy00OGU0OTFlNDYzNGIifQ.KGFBUjghvcmNGDH0eM17S9pWkoLwbvDaDBGAx2AyB41yZ_8-WewTriR08JdjLskw1dsRYpMh9idxQ4BS6xmOCQ";

        final String jwtTokenMsg = " {\"access_token\":\""+jwtString+"\"}";
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setResponseCode(200).setBody(jwtTokenMsg));

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setResponseCode(200).setBody("{\"userId\": \"326aed2a-4c14-42d1-aceb-1feb58fd5c9c\", " +
                        "\"message\": \"authentication success\", \"roles\": \"USER ADMIN\"}"));

        Page page = signIn(this.webClient.getPage("/login/authlogin.html"), "sonam", "password");
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
        client.setMediateToken(true);

        Map<String, String> map = new HashMap<>();
        map.put("settings.client.require-authorization-consent", "true");
        map.put("settings.client.require-proof-key", "false");
        //client.setClientSettings(map.toString());

        //client.setTokenSettings("");

        fillInForm((HtmlPage) clientPage, client);

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setResponseCode(200).setBody(jwtTokenMsg));
        recordedRequest = mockWebServer.takeRequest();
        LOG.info("should be acesstoken path for recordedRequest: {}", recordedRequest.getPath());
        AssertionsForClassTypes.assertThat(recordedRequest.getPath()).startsWith("/oauth2/token?grant_type=client_credentials");
        AssertionsForClassTypes.assertThat(recordedRequest.getMethod()).isEqualTo("POST");

        LOG.info("set client created json response");
        final String clientResponse= "   {" +
                "    \"clientAuthenticationMethods\": \"client_secret_basic,client_secret_jwt\"," +
                "    \"clientSecret\": \"{noop}hellosonam5064\"," +
                "    \"redirectUris\": \"http://localhost:3001/api/auth/callback/myauth\"," +
                "    \"authorizationGrantTypes\": \"refresh_token,client_credentials,authorization_code\"," +
                "    \"clientSettings\": {\"settings.client.require-authorization-consent\":true,\"settings.client.require-proof-key\":false}," +
                "    \"clientId\": \"oauth-client\"," +
                "    \"clientName\": \"64141a86-5e22-4d15-80c3-4fdb69bff4d5\"," +
                "    \"scopes\": \"openid,profile,message.read,email,message.write\"," +
                "    \"mediateToken\": true" +
                "}";

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setResponseCode(200).setBody(clientResponse));

        LOG.info("take the client creation request");
        recordedRequest = mockWebServer.takeRequest();
        LOG.info("should be acesstoken path for recordedRequest: {}", recordedRequest.getPath());
        AssertionsForClassTypes.assertThat(recordedRequest.getPath()).startsWith("/clients");
        AssertionsForClassTypes.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        LOG.info("asserted the path and Http method of POST");*/
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
        HtmlCheckBoxInput mediateToken = page.querySelector("input[name=\"mediateToken\"]");
        //mediateToken.setChecked(true);

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

        //clientSettings.type(client.getClientSettings().toString());
      //  tokenSettings.type(client.getTokenSettings().toString());

        HtmlButton submit = page.querySelector("button");
        LOG.info("sending form with client to create the client");
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
