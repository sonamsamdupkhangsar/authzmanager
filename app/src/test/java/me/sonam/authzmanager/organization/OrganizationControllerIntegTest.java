package me.sonam.authzmanager.organization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.sonam.authzmanager.Application;
import me.sonam.authzmanager.clients.user.OrganizationChoice;
import me.sonam.authzmanager.clients.user.User;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.reactive.function.BodyInserters;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@EnableSpringDataWebSupport
@AutoConfigureWebTestClient
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Application.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OrganizationControllerIntegTest {
    private static final Logger LOG = LoggerFactory.getLogger(OrganizationControllerIntegTest.class);
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
        r.add("auth-server.root", () -> "http://localhost:" + mockWebServer.getPort());
        r.add("oauth2-token-mediator.root", () -> "http://localhost:" + mockWebServer.getPort());
        r.add("organization-rest-service.root", () -> "http://localhost:" + mockWebServer.getPort());
        r.add("role-rest-service.root", () -> "http://localhost:" + mockWebServer.getPort());
        r.add("user-rest-service.root", () -> "http://localhost:" + mockWebServer.getPort());
        r.add("setting-rest-service.root", () -> "http://localhost:" + mockWebServer.getPort());
    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void getOrganizations() throws InterruptedException {
        LOG.info("get organizations");
        UUID userId = UUID.fromString("5d8de63a-0b45-4c33-b9eb-d7fb8d662107");
        Organization organization = new Organization(UUID.randomUUID(), "my company", UUID.randomUUID());

        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        List<UUID> userIdList =  List.of(userId1, userId2);
        RestPage<UUID> userIdPage = new RestPage<>(userIdList, 0,1,2);

        //1 get orgIds of super admin roles
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(userIdPage)));
        User user1 = new User(userId1, "hello@sonam.cloud");
        User user2 = new User(userId2, "bye@sonam.cloud");
        List<User> userList = List.of(user1, user2);

        //2 get organization by ids response
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(userList)));

        // 3 get default organization
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(Map.of("message", Map.of("defaultOrganizationId", organization.getId())))));

        EntityExchangeResult<String> entityExchangeResult = webTestClient.get().uri("/admin/organizations")
                .headers(JwtUtil.addJwt(JwtUtil.jwt("sonam"))).exchange().expectStatus().isOk().expectBody(String.class).returnResult();

        // take request for mocked response of access token
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        //get superadmin org ids
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/authzmanagerroles/users/organizations");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("PUT");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/ids");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/settings/users/"+userId);
    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    //@Test
    //no support for creating organization so leave this test out
    public void getCreateForm() throws InterruptedException {
        LOG.info("get organizations");

        EntityExchangeResult<String> entityExchangeResult = webTestClient.get().uri("/admin/organizations/form")
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();
    }

    //this should fail as I don't support creating organization manually from authzmanager
    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void createOrganization() throws InterruptedException {
        LOG.info("get organizations");

        Organization organization = new Organization(UUID.randomUUID(), "my company", UUID.randomUUID());
        BodyInserters.FormInserter<String> formInserter = BodyInserters.fromFormData("name", organization.getName());


        EntityExchangeResult<String> entityExchangeResult = webTestClient.post().uri("/admin/organizations")
                .body(formInserter)
                .headers(JwtUtil.addJwt(JwtUtil.jwt("sonam")))

                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();

        LOG.info("response: {}", entityExchangeResult.getResponseBody());

        assertThat(entityExchangeResult.getResponseBody()).contains("no support for creating organization");
 }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void updateOrganization() throws InterruptedException {
        LOG.info("get organizations");
        UUID userId = UUID.fromString("5d8de63a-0b45-4c33-b9eb-d7fb8d662107");
        Organization organization = new Organization(UUID.randomUUID(), "my company", UUID.randomUUID());
        BodyInserters.FormInserter<String> formInserter = BodyInserters.fromFormData("name", organization.getName())
                .with("id", organization.getId().toString());
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(Map.of("message", true))));

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(organization)));

        EntityExchangeResult<String> entityExchangeResult = webTestClient.post().uri("/admin/organizations")
                .body(formInserter)
                .headers(JwtUtil.addJwt(JwtUtil.jwt("sonam")))

                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();

        LOG.info("response: {}", entityExchangeResult.getResponseBody());

        assertThat(entityExchangeResult.getResponseBody()).contains("organization updated successfully");

        // take request for mocked response of access token
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/authzmanagerroles/users/"+userId+"/organizations/"+organization.getId());

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("PUT");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations");
    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void updateOrganizationWithDefaultOrganization() throws InterruptedException {
        LOG.info("get organizations");

        UUID userId = UUID.fromString("5d8de63a-0b45-4c33-b9eb-d7fb8d662107");
        Organization organization = new Organization(UUID.randomUUID(), "my company", UUID.randomUUID());
        BodyInserters.FormInserter<String> formInserter = BodyInserters.fromFormData("name", organization.getName())
                .with("id", organization.getId().toString())
                .with("defaultOrganization", "true");

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(Map.of("message", true))));

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(organization)));

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(Map.of("message", "add defaultOrganizationId"))));

        EntityExchangeResult<String> entityExchangeResult = webTestClient.post().uri("/admin/organizations")
                .body(formInserter)
                .headers(JwtUtil.addJwt(JwtUtil.jwt("sonam")))

                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();

        LOG.info("response: {}", entityExchangeResult.getResponseBody());

        assertThat(entityExchangeResult.getResponseBody()).contains("organization updated successfully");

        // take request for mocked response of access token
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/authzmanagerroles/users/"+userId+"/organizations/"+organization.getId());

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("PUT");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("PUT");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/settings/users");
    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void updateOrganizationWithDefaultOrganizationRemoved() throws InterruptedException {
        LOG.info("get organizations");
        UUID userId = UUID.fromString("5d8de63a-0b45-4c33-b9eb-d7fb8d662107");

        Organization organization = new Organization(UUID.randomUUID(), "my company", UUID.randomUUID());
        BodyInserters.FormInserter<String> formInserter = BodyInserters.fromFormData("name", organization.getName())
                .with("id", organization.getId().toString())
                .with("previousDefaultOrganization", "true");

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(Map.of("message", true))));

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(organization)));


        EntityExchangeResult<String> entityExchangeResult = webTestClient.post().uri("/admin/organizations")
                .body(formInserter)
                .headers(JwtUtil.addJwt(JwtUtil.jwt("sonam")))

                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();

        LOG.info("response: {}", entityExchangeResult.getResponseBody());

        assertThat(entityExchangeResult.getResponseBody()).contains("organization updated successfully");

        // take request for mocked response of access token
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/authzmanagerroles/users/"+userId+"/organizations/"+organization.getId());

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("PUT");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations");



    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void getOrganizationById() throws InterruptedException {
        LOG.info("get organization by id");

        UUID userId = UUID.fromString("5d8de63a-0b45-4c33-b9eb-d7fb8d662107");

        Organization organization = new Organization(UUID.randomUUID(), "my company", UUID.randomUUID());
        BodyInserters.FormInserter<String> formInserter = BodyInserters.fromFormData("name", organization.getName());

        //1 get organization by id response
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(organization)));

        //is superAdmin check response
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(Map.of("message", true))));

        //get default org response
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(Map.of("message", Map.of("defaultOrganizationId", organization.getId())))));


        EntityExchangeResult<String> entityExchangeResult = webTestClient.get().uri("/admin/organizations/"+ organization.getId())
                .headers(JwtUtil.addJwt(JwtUtil.jwt("sonam"))) .exchange().expectStatus().isOk().expectBody(String.class).returnResult();

        assertThat(entityExchangeResult.getResponseBody()).contains("<input type=\"text\" class=\"form-control\" id=\"name\" placeholder=\"organization name\" name=\"name\" value=\"my company\" />");

        LOG.info("entity response: {}", entityExchangeResult.getResponseBody());

        assertThat(entityExchangeResult.getResponseBody()).contains("<input type=\"text\" class=\"form-control\" id=\"name\" placeholder=\"organization name\" name=\"name\" value=\"my company\" />");

        // take request for mocked response of access token
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/"+ organization.getId());


        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/authzmanagerroles/users/"+userId+"/organizations/"+organization.getId());

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/settings/users/"+userId);
    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void getRolesForOrganizationId() throws InterruptedException {
        LOG.info("get roles for organization by id");

        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.fromString("5d8de63a-0b45-4c33-b9eb-d7fb8d662107");
        Organization organization = new Organization(orgId, "my company", userId);
        BodyInserters.FormInserter<String> formInserter = BodyInserters.fromFormData("name", organization.getName());

        //1 get default organization response
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(Map.of("message", Map.of("defaultOrganizationId", organization.getId())))));

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(Map.of("message", true))));

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(organization)));

        Role role = new Role(UUID.randomUUID(), "adminRole", null);
        RestPage<Role> restPage = new RestPage<>(List.of(role), 1, 1,1 );

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(restPage)));

        EntityExchangeResult<String> entityExchangeResult = webTestClient.get().uri("/admin/organizations/"+ organization.getId()+"/roles")
                .headers(JwtUtil.addJwt(JwtUtil.jwt("sonam"))).exchange().expectStatus().isOk().expectBody(String.class).returnResult();

        // take request for mocked response of access token
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/settings/users/"+userId);

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/authzmanagerroles/users/"+userId+"/organizations/"+orgId);

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/"+ organization.getId());

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/organizations/"+ organization.getId());
    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
   // @Test
    public void delete() throws InterruptedException {
        LOG.info("delete organization by id");

        Organization organization = new Organization(UUID.randomUUID(), "my company", UUID.randomUUID());
        BodyInserters.FormInserter<String> formInserter = BodyInserters.fromFormData("name", organization.getName());

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody("organization deleted"));

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody("{\"message\": \"default organization removed\"}"));

        EntityExchangeResult<String> entityExchangeResult = webTestClient.delete().uri("/admin/organizations/"+ organization.getId())
                .headers(JwtUtil.addJwt(JwtUtil.jwt("sonam"))).exchange().expectStatus().isOk().expectBody(String.class).returnResult();

        // take request for mocked response of access token
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("DELETE");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/"+ organization.getId());

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("DELETE");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/settings/users");
    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void getUserForOrganizationId() throws InterruptedException {
        LOG.info("get users in organization id");

        UUID userId = UUID.fromString("5d8de63a-0b45-4c33-b9eb-d7fb8d662107");
        UUID orgId = UUID.randomUUID();
        Organization organization = new Organization(orgId, "my company", userId);
        //1
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(Map.of("message", true))));

        //2
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(organization)));

        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        List<UUID> userIdList =  List.of(userId1, userId2);
        RestPage<UUID> userIdPage = new RestPage<>(userIdList, 0,1,2);
        //3
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(userIdPage)));

        User user1 = new User(userId1, "hello@sonam.cloud");
        User user2 = new User(userId2, "bye@sonam.cloud");
        List<User> userList = List.of(user1, user2);
        //RestPage<User> userRestPage = new RestPage<>(userList, 0,1,1,1,1);
        //4
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(userList)));

        EntityExchangeResult<String> entityExchangeResult = webTestClient.get()
                .uri("/admin/organizations/"+ organization.getId()+"/users")
                .headers(JwtUtil.addJwt(JwtUtil.jwt("sonam"))).exchange().expectStatus().isOk().expectBody(String.class).returnResult();

        // take request for mocked response of access token
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/authzmanagerroles/users/"+userId+"/organizations/"+ organization.getId());

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/"+ organization.getId());

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/"+organization.getId()+"/users");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/users/ids/"+userId1+","+userId2);
    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void findUserByAuthenticationId() throws InterruptedException {
        LOG.info("get users in organization id");
        UUID userId = UUID.fromString("5d8de63a-0b45-4c33-b9eb-d7fb8d662107");

        Organization organization = new Organization(UUID.randomUUID(), "my company", UUID.randomUUID());
        //1 is user superamdin response
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(Map.of("message", true))));

        //2 get org by id
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(organization)));

        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        List<UUID> userIdList =  List.of(userId1, userId2);
        RestPage<UUID> userIdPage = new RestPage<>(userIdList, 0,1,2);
        //3 get user ids in organization response
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(userIdPage)));
        User user1 = new User(userId1, "hello@sonam.cloud");
        User user2 = new User(userId2, "bye@sonam.cloud");
        List<User> userList = List.of(user1, user2);

        //4 get user for batch of ids response
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(userList)));

        User user = new User(userId1, "hello@sonam.cloud");
        //5 find by authentication id response
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(user)));
        //6 user exists in organization response
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody("{\"message\": \"true\"}"));

        BodyInserters.FormInserter<String> formInserter = BodyInserters.fromFormData("username", "johnbadmash");

        EntityExchangeResult<String> entityExchangeResult = webTestClient.post()
                .uri("/admin/organizations/"+ organization.getId()+"/users")
                .headers(JwtUtil.addJwt(JwtUtil.jwt("sonam"))).body(formInserter)
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();

        // take request for mocked response of access token
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/authzmanagerroles/users/"+userId+"/organizations/"+ organization.getId());

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/"+organization.getId());

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/"+organization.getId()+"/users");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/users/ids/"+userId1+","+userId2);

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/users/profile/authentication-id/johnbadmash");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/users/"+userId1+"/ids");
    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void addUserToOrganization() throws InterruptedException {
        LOG.info("get users in organization id");

        UUID userId = UUID.fromString("5d8de63a-0b45-4c33-b9eb-d7fb8d662107");
        // this is the post body for the endpoint
        User user = new User(userId, "user1@sonam.cloud");
        UUID orgId = UUID.randomUUID();
        OrganizationChoice organizationChoice = new OrganizationChoice();
        organizationChoice.setOrganizationId(orgId);
        user.setOrganizationChoice(organizationChoice);

       //1

        Organization organization = new Organization(user.getOrganizationChoice().getOrganizationId(), "my company",
                UUID.fromString("5d8de63a-0b45-4c33-b9eb-d7fb8d662107"));
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(organization)));

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(Map.of("message", true))));

        //2
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(Map.of("message", "added user to organization"))));

        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        List<UUID> userIdList =  List.of(userId1, userId2);
        RestPage<UUID> userIdPage = new RestPage<>(userIdList, 0,1,2);

        //3
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(userIdPage)));

        User user1 = new User(userId1, "hello@sonam.cloud");
        User user2 = new User(userId2, "bye@sonam.cloud");
        List<User> userList = List.of(user1, user2);
        //RestPage<User> userRestPage = new RestPage<>(userList, 0,1,1,1,1);
        //4
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(userList)));


        BodyInserters.FormInserter<String> formInserter = BodyInserters.fromFormData("id", user.getId().toString())
                .with("organizationChoice.organizationId", user.getOrganizationChoice().getOrganizationId().toString())
                .with("organizationChoice.selected", "true");

        EntityExchangeResult<String> entityExchangeResult = webTestClient.post()
                .uri("/admin/organizations/"+ organizationChoice.getOrganizationId()+"/users/add")
                .headers(JwtUtil.addJwt(JwtUtil.jwt("sonam"))).body(formInserter)
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();

        // take request for mocked response of access token
        //get org by id
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/"+orgId);

        // is user a superadmin in org-id
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/authzmanagerroles/users/"+user.getId()+"/organizations/"+orgId);

        // add user to organization
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/users");

        // get user ids in organization
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/"+orgId + "/users");

        //get user by batch of ids
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/users/ids/"+userId1+","+userId2);

    }

    /**
     * This tests the user adding to organization when the user is searched by auth-id and is adding from the form.
     * Therefore this uses the postMapping with selection field on form.
     * @throws InterruptedException
     */
    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void removeUserFromOrganization() throws InterruptedException {
        LOG.info("remove user from organization");

        UUID orgId = UUID.randomUUID();
        UUID loggedInUserId = UUID.fromString("5d8de63a-0b45-4c33-b9eb-d7fb8d662107");
        // this is the post body for the endpoint
        User user = new User(UUID.randomUUID(), "user1@sonam.cloud");
        OrganizationChoice organizationChoice = new OrganizationChoice();
        organizationChoice.setOrganizationId(orgId);
        user.setOrganizationChoice(organizationChoice);

        Organization organization = new Organization(user.getOrganizationChoice().getOrganizationId(), "my company",
                UUID.fromString("5d8de63a-0b45-4c33-b9eb-d7fb8d662107"));

        //1 get organization by id
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(organization)));

        //2 is superAdmin
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(Map.of("message", true))));

        //3 remove user from organization response
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(Map.of("message", "user removed from organization"))));

        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        List<UUID> userIdList =  List.of(userId1, userId2);
        RestPage<UUID> userIdPage = new RestPage<>(userIdList, 0,1,2);
        //4 get userIds in organization
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(userIdPage)));

        User user1 = new User(userId1, "hello@sonam.cloud");
        User user2 = new User(userId2, "bye@sonam.cloud");
        List<User> userList = List.of(user1, user2);
        //RestPage<User> userRestPage = new RestPage<>(userList, 0,1,1,1,1);
        //5 get user by batch of ids response
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(userList)));

        BodyInserters.FormInserter<String> formInserter = BodyInserters.fromFormData("id", user.getId().toString())
                .with("organizationChoice.organizationId", user.getOrganizationChoice().getOrganizationId().toString())
                .with("organizationChoice.selected", "false");

        EntityExchangeResult<String> entityExchangeResult = webTestClient.post()
                .uri("/admin/organizations/"+ organizationChoice.getOrganizationId()+"/users/add")
                .headers(JwtUtil.addJwt(JwtUtil.jwt("sonam")))
                .body(formInserter)
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();

        // take request for mocked response of access token
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/"+organization.getId());

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/authzmanagerroles/users/"+loggedInUserId+"/organizations/"+organization.getId());

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("DELETE");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/"+user.getOrganizationChoice().getOrganizationId()+"/users/"+user.getId());

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/"+user.getOrganizationChoice().getOrganizationId()+"/users");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/users/ids/"+userId1+","+userId2);
    }

    private static String getJson(Object object) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        try {
            String json = objectMapper.writeValueAsString(object);
            LOG.info("json for object: {}", json);
            return json;
        } catch (JsonProcessingException e) {
            LOG.error("error occurred", e);
            return null;
        }
    }

    private static String getSimpleJson(Object object) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            String json = objectMapper.writeValueAsString(object);
            LOG.info("json for object: {}", json);
            return json;
        } catch (JsonProcessingException e) {
            LOG.error("error occurred", e);
            return null;
        }
    }
}
