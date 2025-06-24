package me.sonam.authzmanager.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.sonam.authzmanager.Application;
import me.sonam.authzmanager.controller.admin.clients.ClientController;
import me.sonam.authzmanager.controller.signup.UserSignup;
import me.sonam.authzmanager.oauth2.util.RegisteredClientUtil;
import me.sonam.authzmanager.security.WithMockCustomUser;
import me.sonam.authzmanager.tokenfilter.TokenService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Application.class})
public class UserSignupIntegTest {
    private static final Logger LOG = LoggerFactory.getLogger(UserSignupIntegTest.class);

    @Autowired
    private ClientController clientController;
    private String userId = UUID.randomUUID().toString();

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private MockMvc mockMvc;
    private static MockWebServer mockWebServer;

    private RegisteredClientUtil registeredClientUtil = new RegisteredClientUtil();
    private final String token = "{\"access_token\": \"eyJraWQiOiJlOGQ3MjIzMC1iMDgwLTRhZjEtODFkOC0zMzE3NmNhMTM5ODIiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJhdWQiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJuYmYiOjE3MTQ3NTY2ODIsImlzcyI6Imh0dHA6Ly9teS1zZXJ2ZXI6OTAwMSIsImV4cCI6MTcxNDc1Njk4MiwiaWF0IjoxNzE0NzU2NjgyLCJqdGkiOiI0NDBlZDY0My00MzdkLTRjOTMtYTZkMi1jNzYxNjFlNDRlZjUifQ.fjqgoczZbbmcnvYpVN4yakpbplp7EkDyxslvar5nXBFa6mgIFcZa29fwIKfcie3oUMQ8MDWxayak5PZ_QIuHwTvKSWHs0WL91ljf-GT1sPi1b4gDKf0rJOwi0ClcoTCRIx9-WGR6t2BBR1Rk6RGF2MW7xKw8M-RMac2A2mPEPJqoh4Pky1KgxhZpEXixegpAdQIvBgc0KBZeQme-ZzTYugB8EPUmGpMlfd-zX_vcR1ijxi8e-LRRJMqmGkc9GXfrH7MOKNQ_nu6pc6Gish2v_iuUEcpPHXrfqzGb9IHCLvfuLSaTDcYKYjQaEUAp-1uDW8-5posjiUV2eBiU48ajYg\", \"token_type\":\"Bearer\", \"expires_in\":\"299\"}";


    @MockBean
    private TokenService tokenService;

    @BeforeEach
    public void setTokenServiceMockBehavior() {
        OAuth2AccessToken oAuth2AccessToken = mock(OAuth2AccessToken.class);

        when(tokenService.getAccessToken(any())).thenReturn(oAuth2AccessToken);
        when( oAuth2AccessToken.getTokenValue()).thenReturn("sonamstoken");
        when(tokenService.getAccessToken()).thenReturn("dummytoken");
    }

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

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) throws IOException {
        r.add("auth-server.root", () -> "http://localhost:" + mockWebServer.getPort());
        r.add("oauth2-token-mediator.root", () -> "http://localhost:" + mockWebServer.getPort());
        r.add("organization-rest-service.root", () -> "http://localhost:" + mockWebServer.getPort());
        r.add("role-rest-service.root", () -> "http://localhost:" + mockWebServer.getPort());
        r.add("user-rest-service.root", () -> "http://localhost:" + mockWebServer.getPort());
    }

    @Test
    public void userSignup() throws Exception {
        UserSignup userSignup = new UserSignup("Sonam", "Wangyal", "mugambo@1234sonam.com", "mugambo", "hello".toCharArray(), false);

        Map<String, Object> map = new HashMap<>();
        map.put("firstName", "Sonam");
        map.put("lastName", "Wangyal");
        map.put("email", "SOnAm@123zuma.com");
        map.put("authenticationId", "soNAM");
        map.put("password", "12345");
        map.put("active", false);

        //on signup we need to return a token because the user is not logged-in.  The app will generate a token and send to /users endpoint
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(token));

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody("{\"message\": \"user created\"}"));

        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post("/signup")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("firstName", userSignup.getFirstName())
                        .param("lastName", userSignup.getLastName())
                        .param("email", userSignup.getEmail())
                        .param("authenticationId", userSignup.getAuthenticationId())
                        .param("password", "1234567890")
                        .param("active", "false")).andDo(print()).andExpect(status().isOk()).andReturn();

        LOG.info("mvcResult: {}", mvcResult.getResponse());
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/users");

    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void admInUserSignup() throws Exception {
        UserSignup userSignup = new UserSignup("Sonam", "Wangyal", "mugambo@1234sonam.com", "mugambo", "hello".toCharArray(), false);

        Map<String, Object> map = new HashMap<>();
        map.put("firstName", "Sonam");
        map.put("lastName", "Wangyal");
        map.put("email", "SOnAm@123zuma.com");
        map.put("authenticationId", "soNAM");
        map.put("password", "12345");
        map.put("active", false);


      /*  mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setResponseCode(200).setBody(token));*/

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody("{\"message\": \"user created\"}"));

        UUID orgId = UUID.randomUUID();
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post("/admin/organizations/default/users")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("firstName", userSignup.getFirstName())
                .param("lastName", userSignup.getLastName())
                .param("email", userSignup.getEmail())
                .param("authenticationId", userSignup.getAuthenticationId())
                .param("password", "1234567890")
                        .param("organizationId", orgId.toString())
                .param("active", "false")).andDo(print()).andExpect(status().isOk()).andReturn();

        LOG.info("mvcResult: {}", mvcResult.getResponse());
        RecordedRequest recordedRequest = mockWebServer.takeRequest();

        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/"+orgId.toString());

    }


    public String getJson(Object o) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(o);
    }
}

