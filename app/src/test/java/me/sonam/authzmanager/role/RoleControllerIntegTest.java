package me.sonam.authzmanager.role;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.sonam.authzmanager.Application;
import me.sonam.authzmanager.controller.admin.organization.Organization;
import me.sonam.authzmanager.controller.admin.roles.Role;
import me.sonam.authzmanager.rest.RestPage;
import me.sonam.authzmanager.security.WithMockCustomUser;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.util.JwtUtil;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.assertj.core.api.Assertions;
import org.htmlunit.WebClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.core.io.Resource;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.reactive.function.BodyInserters;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@EnableSpringDataWebSupport
@AutoConfigureWebTestClient
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

    @MockitoBean
    private TokenService tokenService;
    @Autowired
    WebApplicationContext context;

    @org.junit.jupiter.api.BeforeEach
    public void setup() {
        this.webTestClient = MockMvcWebTestClient.bindToApplicationContext(context)
                // add Spring Security test Support
                .apply(springSecurity())
                .configureClient()
                .build();
    }
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

    private static String host;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) throws IOException {
        r.add("auth-server.root", () -> "http://localhost:"+mockWebServer.getPort());
        r.add("oauth2-token-mediator.root", () -> "http://localhost:"+mockWebServer.getPort());
        r.add("organization-rest-service.root", () -> "http://localhost:" + mockWebServer.getPort());
        r.add("role-rest-service.root", () -> "http://localhost:" + mockWebServer.getPort());
        r.add("user-rest-service.root", () -> "http://localhost:" + mockWebServer.getPort());
        r.add("setting-rest-service.root", ()->"http://localhost:"+mockWebServer.getPort());
    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void getRolesByOrganizationId() throws InterruptedException {
        LOG.info("get roles by userId");

        UUID userId = UUID.fromString("5d8de63a-0b45-4c33-b9eb-d7fb8d662107");
        UUID organizationId = UUID.randomUUID();
        Role role = new Role(UUID.randomUUID(), "adminRole", null);

        //1 get default org
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(Map.of("message", Map.of("defaultOrganizationId", organizationId)))));

        //2 is superAdmin check response
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(Map.of("message", true))));

        RestPage<Role> restPage = new RestPage<>(List.of(role), 1, 1,1 );

        //3 get roles by organization response
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(restPage)));

        EntityExchangeResult<String> entityExchangeResult = webTestClient.get().uri("/admin/roles")
                .headers(JwtUtil.addJwt(JwtUtil.jwt("sonam"))).exchange().expectStatus().isOk().expectBody(String.class).returnResult();
        LOG.info("response: {}", entityExchangeResult.getResponseBody());

        // take request for mocked response of access token
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/settings/users/"+userId);

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/authzmanagerroles/users/"+userId+"/organizations/"+organizationId);

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/organizations/"+organizationId);
    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void getCreateForm() {
        EntityExchangeResult<String> entityExchangeResult = webTestClient.get().uri("/admin/roles/new")
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();
        LOG.info("response: {}", entityExchangeResult.getResponseBody());
    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void createNewRole() throws InterruptedException {
        LOG.info("create new role");
        saveRole();
    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void getRoleById() throws InterruptedException {
        LOG.info("get role by id");

        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.fromString("5d8de63a-0b45-4c33-b9eb-d7fb8d662107");

        Role role = new Role(UUID.randomUUID(), "adminRole", organizationId);

        //1 get default org
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(Map.of("message", Map.of("defaultOrganizationId", organizationId)))));

        //2 is superAdmin check response
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(Map.of("message", true))));

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(role)));

        Organization organization = new Organization(UUID.randomUUID(), "my company", UUID.randomUUID());
        List<Organization> list = List.of(organization);
        RestPage<Organization> CustomRestPage = new RestPage<Organization>(list, 0, 1,1);

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(CustomRestPage)));

        EntityExchangeResult<String> entityExchangeResult = webTestClient.get().uri("/admin/roles/"+role.getId())
                .headers(JwtUtil.addJwt(JwtUtil.jwt("sonam"))) .exchange().expectStatus().isOk().expectBody(String.class).returnResult();
        LOG.info("response: {}", entityExchangeResult.getResponseBody());

        // take request for mocked response of access token
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/settings/users/"+userId);

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/authzmanagerroles/users/"+userId+"/organizations/"+organizationId);

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/"+role.getId());
    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void delete() throws InterruptedException {
        LOG.info("delete role by id");

        UUID organizationId = UUID.randomUUID();

        Role role = new Role(UUID.randomUUID(), "adminRole", organizationId);

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody("role and organization association deleted"));

        EntityExchangeResult<String> entityExchangeResult = webTestClient.delete().uri("/admin/roles/"+role.getId())
                .headers(JwtUtil.addJwt(JwtUtil.jwt("sonam"))).exchange().expectStatus().isOk().expectBody(String.class).returnResult();
        LOG.info("response: {}", entityExchangeResult.getResponseBody());

        // take request for mocked response of access token
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("DELETE");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/"+role.getId());
    }

    private void saveRole() throws InterruptedException {
        UUID userId = UUID.fromString("5d8de63a-0b45-4c33-b9eb-d7fb8d662107");
        UUID organizationId = UUID.randomUUID();
       Role role = new Role(UUID.randomUUID(), "adminRole", organizationId);

       //1 get default org
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(Map.of("message", Map.of("defaultOrganizationId", organizationId)))));

        //2 is superAdmin check response
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(Map.of("message", true))));

        //3 update role
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(role)));

       Organization organization = new Organization(organizationId, "my company", UUID.randomUUID());
       List<Organization> list = List.of(organization);
       RestPage<Organization> restPage = new RestPage<Organization>(list, 1, 1,1);


       BodyInserters.FormInserter<String> formInserter = BodyInserters.fromFormData("name", role.getName());

       EntityExchangeResult<String> entityExchangeResult = webTestClient.post().uri("/admin/roles")
               .headers(JwtUtil.addJwt(JwtUtil.jwt("sonam"))) .body(formInserter)
               .exchange().expectStatus().isOk().expectBody(String.class).returnResult();
       LOG.info("response: {}", entityExchangeResult.getResponseBody());

       // take request for mocked response of access token
       RecordedRequest recordedRequest = mockWebServer.takeRequest();
       Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
       Assertions.assertThat(recordedRequest.getPath()).startsWith("/settings/users/"+userId);

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/authzmanagerroles/users/"+userId+"/organizations/"+organizationId);

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles");

    }

    private static String getJson(Object object) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        try {
            String json = objectMapper.writeValueAsString(object);
            LOG.info("json for object: {}", json);
            return json;
        } catch (JsonProcessingException e) {
            LOG.error("error occured", e);
            return null;
        }
    }
}
