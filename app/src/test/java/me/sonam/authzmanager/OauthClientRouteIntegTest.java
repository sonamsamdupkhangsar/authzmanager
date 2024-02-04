package me.sonam.authzmanager;

/*

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
*/

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class OauthClientRouteIntegTest {
    private static final Logger LOG = LoggerFactory.getLogger(OauthClientRouteIntegTest.class);

    //private static MockWebServer mockWebServer;

    @Autowired
    private WebTestClient webTestClient;

    private String messageClient = "messaging-client";

    private String clientSecret = "secret";
    private String base64ClientSecret = Base64.getEncoder().encodeToString(new StringBuilder(messageClient)
            .append(":").append(clientSecret).toString().getBytes());
    private UUID clientId = UUID.randomUUID();
    @Value("classpath:client-credential-access-token.json")
    private Resource refreshTokenResource;

  /*  @BeforeAll
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

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) throws IOException {
        r.add("authentication-rest-service.root", () -> "http://localhost:"+mockWebServer.getPort());
        r.add("account-rest-service.root", () -> "http://localhost:"+mockWebServer.getPort());
        r.add("auth-server.root", () -> "http://localhost:"+ mockWebServer.getPort());
    }
*/
    @Test
    public void createClientTest() throws Exception {
        saveClient();
    }

    private void saveClient() throws  Exception {
        UUID userId = UUID.randomUUID();

        LOG.info("request oauth access token first");
        final String accessToken = "eyJraWQiOiJmNGUzMjUwYi05NWE3LTRiODYtYjAwMS02YmJjMzYwZjNlMDIiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI4ZmZiNTBhZC0xM2ZlLTQ1NjUtYjQ1Mi1hMDZhZjI3Njk1N2QiLCJhdWQiOiI4ZmZiNTBhZC0xM2ZlLTQ1NjUtYjQ1Mi1hMDZhZjI3Njk1N2QiLCJuYmYiOjE3MDY0MTQyNDgsInNjb3BlIjpbIm1lc3NhZ2UucmVhZCIsIm1lc3NhZ2Uud3JpdGUiXSwiaXNzIjoiaHR0cDovL215LXNlcnZlcjo5MDAxIiwiZXhwIjoxNzA2NDE0NTQ4LCJpYXQiOjE3MDY0MTQyNDh9.dcDcpHqyh37SkQKdp1yhgvrb_yGVN0iZh8y2ssymoBVrB_dZkPK0BBIZRyV0ueATX8v0xfZlHgtfaG4QOdNkKIuc0EOiLLxoqpqAp5s0oMPgaXfWiKSZ2Zw6r168yu91bwg8saRmYJlKvm87-lLJM86EjpPOtZILr9cb9X-E3htfZcR-1QjmDT9mGpyI94ECALK8b7iosgAzmdagyc30iwjsKm20r5niVVfMQc0RhSrSNm99TFrN8Iic0dkgaUPiDbU8OAWuMKFdUKjqYBk66TDb1PTBtj2nhiPNG0AAD2UbLC2qpD2HvHLWuZmS6_DEpxeXT95Qxhzisi5_QLZ5hg";
        String jsonTokenMap = "{\"access_token\": "+"\""+accessToken+"\"}";
        LOG.info("jsonMap: {}", jsonTokenMap);

        LOG.info("now make a request to create a client");
        var requestBody = Map.of("clientId", clientId, "clientSecret", "{noop}secret",
                "clientName", "Blog Application",
                "clientAuthenticationMethods", "client_secret_basic,client_secret_jwt",
                "authorizationGrantTypes", "authorization_code,refresh_token,client_credentials",
                "redirectUris", "http://127.0.0.1:8080/login/oauth2/code/my-client-oidc,http://127.0.0.1:8080/authorized",
                "scopes", "openid,profile,message.read,message.write",
                "clientSettings", Map.of("settings.client.require-proof-key", "false", "settings.client.require-authorization-consent", "true"),
                "mediateToken", "true",
                "userId", userId.toString());

/*
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setResponseCode(200)
                .setBody(jsonTokenMap));
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setResponseCode(200).setBody("{\"message\": \"saved client, count of client by clientId: 1\"}"));

        WebTestClient.ResponseSpec responseSpec = webTestClient.post().uri("/authzmanager/clients").bodyValue(requestBody)
                .exchange().expectStatus().isCreated();
        assertThat(responseSpec.expectBody(String.class).returnResult().getResponseBody()).isNotEmpty();

        // take request for mocked response of access token
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath()).startsWith("/oauth2/token?grant_type=client_credentials");

        LOG.info("take request for mocked response to token-mediator for client save");
        recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath()).startsWith("/authzmanager/clients");*/
    }
}