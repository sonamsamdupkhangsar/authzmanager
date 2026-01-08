package me.sonam.authzmanager.settings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.sonam.authzmanager.Application;
import me.sonam.authzmanager.clients.user.User;
import me.sonam.authzmanager.controller.admin.clients.ClientController;
import me.sonam.authzmanager.controller.admin.organization.Organization;
import me.sonam.authzmanager.rest.RestPage;
import me.sonam.authzmanager.security.WithMockCustomUser;
import me.sonam.authzmanager.tokenfilter.TokenService;
import me.sonam.authzmanager.util.JwtUtil;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@EnableSpringDataWebSupport
@AutoConfigureWebTestClient
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Application.class})
public class SettingsControllerIntegTest {
    private static final Logger LOG = LoggerFactory.getLogger(SettingsControllerIntegTest.class);

    @Autowired
    private ClientController clientController;
    private String userId = UUID.randomUUID().toString();

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private TokenService tokenService;

    @Autowired
    private MockMvc mockMvc;
    private static MockWebServer mockWebServer;
    @MockitoBean
    ReactiveJwtDecoder jwtDecoder;
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

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) throws IOException {
        r.add("auth-server.root", () -> "http://localhost:"+mockWebServer.getPort());
        r.add("oauth2-token-mediator.root", () -> "http://localhost:"+mockWebServer.getPort());
        r.add("organization-rest-service.root", () -> "http://localhost:" + mockWebServer.getPort());
        r.add("role-rest-service.root", () -> "http://localhost:" + mockWebServer.getPort());
        r.add("setting-rest-service.root", () -> "http://localhost:" + mockWebServer.getPort());
        r.add("user-rest-service.root", () -> "http://localhost:" + mockWebServer.getPort());
    }

    private void setMockResponsesGetUsersForDefaultOrganization(UUID defaultOrgId, UUID superAdminAuthzManagerRoleId) {
        if (defaultOrgId == null){
            mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                    .setResponseCode(200).setBody(getJson(Map.of("message", superAdminAuthzManagerRoleId))));

            mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                    .setResponseCode(400).setBody(getJson(Map.of("error", "No Default organization found"))));
        }
        else {
            mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                    .setResponseCode(200).setBody(getJson(Map.of("message", superAdminAuthzManagerRoleId))));
            mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                    .setResponseCode(200).setBody(getJson(Map.of("message", Map.of("defaultOrganizationId", defaultOrgId)))));


            mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                    .setResponseCode(200).setBody(getJson(new Organization(defaultOrgId, "myorg", UUID.fromString(userId)))));

            UUID userId1 = UUID.randomUUID();
            UUID userId2 = UUID.randomUUID();
            UUID userId3 = UUID.randomUUID();

            LOG.info("set restPage of userIds for response");
            RestPage<UUID> restPage = new RestPage<>(List.of(userId1, userId2, userId3), 1, 5, 3, 3);
            mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                    .setResponseCode(200).setBody(getJson(restPage)));

            User user1 = new User();
            user1.setId(userId1);
            user1.setFirstName("Sonam");

            User user2 = new User();
            user2.setId(userId2);
            user2.setFirstName("Tashi");

            User user3 = new User();
            user3.setId(userId3);
            user3.setFirstName("Kalsang");

            LOG.info("set json for list of users for the ids");
            mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                    .setResponseCode(200).setBody(getJson(List.of(user1, user2, user3))));

            UUID userId4 = UUID.randomUUID();

            UUID authzManagerRoleOrganizationId1 = UUID.randomUUID();
            UUID authzManagerRoleOrganizationId2 = UUID.randomUUID();

            Map<UUID, UUID> uuidBooleanMap = Map.of(userId1, authzManagerRoleOrganizationId1,
                    userId2, authzManagerRoleOrganizationId2);
            LOG.info("set json for superAdmin map");
            mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                    .setResponseCode(200).setBody(getJson(uuidBooleanMap)));
        }

    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void getUsersWithoutDefaultOrganization() throws InterruptedException {
        LOG.info("get client by client's id");

        UUID defaultOrgId = UUID.randomUUID();

        UUID superAdminAuthzManagerRoleId = UUID.randomUUID();

        setMockResponsesGetUsersForDefaultOrganization(null, superAdminAuthzManagerRoleId);

        String authenticationId = "dave";
        Jwt jwt = JwtUtil.jwt(authenticationId);

        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        EntityExchangeResult<String> entityExchangeResult = webTestClient.get()
                .uri("/admin/settings")
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();
        LOG.info("response: {}", entityExchangeResult.getResponseBody());

        takeRequestForGetUsersForDefaultOrganization(null);
    }


    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void getUsersForDefaultOrganization() throws InterruptedException {
        LOG.info("get client by client's id");

        UUID defaultOrgId = UUID.randomUUID();

        UUID superAdminAuthzManagerRoleId = UUID.randomUUID();

        setMockResponsesGetUsersForDefaultOrganization(defaultOrgId, superAdminAuthzManagerRoleId);

        String authenticationId = "dave";
        Jwt jwt = JwtUtil.jwt(authenticationId);

        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        EntityExchangeResult<String> entityExchangeResult = webTestClient.get()
                .uri("/admin/settings")
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();
        LOG.info("response: {}", entityExchangeResult.getResponseBody());

        takeRequestForGetUsersForDefaultOrganization(defaultOrgId);
    }

    private void takeRequestForGetUsersForDefaultOrganization(UUID defaultOrgId) throws InterruptedException {


        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("PUT");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/authzmanagerroles/name");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/settings/users");

        if (defaultOrgId != null) {
            recordedRequest = mockWebServer.takeRequest();
            Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/" + defaultOrgId);

            recordedRequest = mockWebServer.takeRequest();
            Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/" + defaultOrgId + "/users");


            recordedRequest = mockWebServer.takeRequest();
            Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
            Assertions.assertThat(recordedRequest.getPath()).startsWith("/users/ids/");

            recordedRequest = mockWebServer.takeRequest();
            Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("PUT");
            Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/authzmanagerroles/users/organizations/" + defaultOrgId);
        }

    }

    //set user as superadmin
    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void setUserSuperAdmin() throws InterruptedException {
        LOG.info("set user as superadmin");

        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID authzManagerRoleId = UUID.randomUUID();

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(Map.of("id", UUID.randomUUID(), "authzManagerRoleId",
                        authzManagerRoleId, "userId", userId, "organizationId", organizationId))));

        setMockResponsesGetUsersForDefaultOrganization(organizationId, authzManagerRoleId);

        EntityExchangeResult<String> entityExchangeResult = webTestClient.post()
                .uri("/admin/settings?authzManagerRoleId="+authzManagerRoleId+"&userId="+userId+"&organizationId="+organizationId)
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();
        LOG.info("response: {}", entityExchangeResult.getResponseBody());

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/authzmanagerroles/users/organizations");
    }


    //set user as superadmin
    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void deleteUserSuperAdmin() throws InterruptedException {
        LOG.info("delete authzManagerRoleOrganization by id");

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(Map.of("message", "User removed from AuthzManagerRoleOrganization"))));

        UUID authzManagerRoleOrganizationId = UUID.randomUUID();

        EntityExchangeResult<String> entityExchangeResult = webTestClient.delete()
                .uri("/admin/settings?authzManagerRoleOrganizationId="+authzManagerRoleOrganizationId)
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();
        LOG.info("response: {}", entityExchangeResult.getResponseBody());

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("DELETE");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/authzmanagerroles/users/organizations/"+authzManagerRoleOrganizationId);
    }

    //set user as superadmin
    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void findUserByAuthenticationId() throws InterruptedException {
        LOG.info("delete authzManagerRoleOrganization by id");

        UUID defaultOrgId = UUID.randomUUID();

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(new Organization(defaultOrgId, "myorg", UUID.fromString(userId)))));

        final String userName = "Tashi";
        User user = new User(UUID.randomUUID());
        user.setFirstName("Tashi");

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(user)));

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(Map.of("message", true))));


        UUID superAdminAuthzManagerRoleId = UUID.randomUUID();
        //setMockResponsesGetUsersForDefaultOrganization(defaultOrgId, superAdminAuthzManagerRoleId);

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(Map.of("message", superAdminAuthzManagerRoleId))));

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(Map.of("message", Map.of("defaultOrganizationId", defaultOrgId)))));

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(new Organization(defaultOrgId, "myorg", UUID.fromString(userId)))));

        UUID userId1 = UUID.randomUUID();
        UUID authzManagerRoleOrganizationId1 = UUID.randomUUID();
        UUID authzManagerRoleOrganizationId2 = UUID.randomUUID();

        Map<UUID, UUID> uuidBooleanMap = Map.of(userId1, authzManagerRoleOrganizationId1);
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(uuidBooleanMap)));

        EntityExchangeResult<String> entityExchangeResult = webTestClient.post()
                .uri("/admin/settings/"+defaultOrgId+"/users")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("username", userName))

                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();
        LOG.info("response: {}", entityExchangeResult.getResponseBody());

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        //getOrganizationById
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/"+defaultOrgId);

        //findByAuthenticationProfileSearch
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/users/profile/authentication-id/"+userName
                +"?ignoreSearchable=true");

        //userExistsInOrganization by userId
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/"+defaultOrgId+"/users/"+user.getId());

        //getAuthzManagerRoleByName
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("PUT");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/authzmanagerroles/name");

        //getDefaultOrganization for logged-in user-id
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/settings/users");

        //getOrganizationById
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/"+defaultOrgId);

        //areUsersSuperAdminInDefaultOrgId
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("PUT");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/authzmanagerroles/users/organizations/"+defaultOrgId);

        assertThat(entityExchangeResult.getResponseBody()).contains("Tashi");
        assertThat(entityExchangeResult.getResponseBody()).contains("Make SuperAdmin");
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
