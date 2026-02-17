package me.sonam.authzmanager.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.sonam.authzmanager.Application;
import me.sonam.authzmanager.controller.admin.clients.ClientOrganizationUserController;
import me.sonam.authzmanager.oauth2.OauthClient;
import me.sonam.authzmanager.oauth2.OidcScopes;
import me.sonam.authzmanager.oauth2.ClientSettings;
import me.sonam.authzmanager.oauth2.RegisteredClient;
import me.sonam.authzmanager.oauth2.util.RegisteredClientUtil;
import me.sonam.authzmanager.controller.admin.organization.Organization;
import me.sonam.authzmanager.controller.admin.roles.Role;
import me.sonam.authzmanager.controller.admin.clients.ClientController;
import me.sonam.authzmanager.controller.admin.clients.carrier.ClientOrganizationUserWithRole;
import me.sonam.authzmanager.controller.admin.clients.carrier.User;
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
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.reactive.function.BodyInserters;

import java.io.IOException;
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
@SpringBootTest(classes = {Application.class})
public class ClientOrganizationUserControllerIntegTest {
    private static final Logger LOG = LoggerFactory.getLogger(ClientOrganizationUserControllerIntegTest.class);

    @Autowired
    private ClientController clientController;
    private String userId = UUID.randomUUID().toString();

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private MockMvc mockMvc;
    private static MockWebServer mockWebServer;

    private RegisteredClientUtil registeredClientUtil = new RegisteredClientUtil();

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

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) throws IOException {
        r.add("auth-server.root", () -> "http://localhost:" + mockWebServer.getPort());
        r.add("oauth2-token-mediator.root", () -> "http://localhost:" + mockWebServer.getPort());
        r.add("organization-rest-service.root", () -> "http://localhost:" + mockWebServer.getPort());
        r.add("role-rest-service.root", () -> "http://localhost:" + mockWebServer.getPort());
        r.add("user-rest-service.root", () -> "http://localhost:" + mockWebServer.getPort());
    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void deleteClientOrganizationUserRole() throws InterruptedException {
        LOG.info("delete clientOrganizationUserRole");

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody("roleClientOrganizationUser deleted"));

        OauthClient oauthClient = getOauthClient();
        oauthClient.setId(UUID.randomUUID().toString());
        UUID organizationId = UUID.randomUUID();

        setMockResponse(oauthClient, organizationId);

        UUID id = UUID.randomUUID();

        EntityExchangeResult<String> entityExchangeResult = webTestClient.delete()
                .uri("/admin/clients/"+oauthClient.getId()+"/organizations/users/roles/"+id)
               // .uri("/admin/clients/"+oauthClient.getId()+"/users/client-organization-user-role/"+roleId)
                //.headers(JwtUtil.addJwt(JwtUtil.jwt("sonam")))
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();
        LOG.info("response: {}", entityExchangeResult.getResponseBody());

        // take request for mocked response of access token
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("DELETE");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/clients/organizations/users/roles/"+id);

        takeRequests(mockWebServer, oauthClient, organizationId);
    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void addClientOrganizationUserRole() throws InterruptedException {
        LOG.info("add client organization user to role");

        OauthClient oauthClient = getOauthClient();
        oauthClient.setId(UUID.randomUUID().toString());
        UUID organizationId = UUID.randomUUID();

        User user = new User(UUID.randomUUID(), new Role(UUID.randomUUID(), "super role", organizationId));
        ClientOrganizationUserWithRole clientOrganizationUserWithRole =
                new ClientOrganizationUserWithRole(UUID.fromString(oauthClient.getId()), organizationId, user, new Role(UUID.randomUUID(), "super role", organizationId));


        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(clientOrganizationUserWithRole)));

        setMockResponse(oauthClient, organizationId);

//        UUID organizationId = UUID.fromString("2cc2cf6e-7adb-4c31-a4c6-906d6b024a1e");
        BodyInserters.FormInserter<String> formInserter = BodyInserters.fromFormData("clientId", oauthClient.getId())
                .with("organizationId", organizationId.toString())
                .with("id", "f8b36547-9f1e-4905-b726-e50e76a9076b")
                .with("user.id", "1f442dab-96a3-459e-8605-7f5cd5f82e25")
                .with("role.id", "40a8a40d-1abf-4e65-9fea-fe5db321ead8");


        EntityExchangeResult<String> entityExchangeResult = webTestClient.post().uri(
                "/admin/clients/"+oauthClient.getId()+"/organizations/users/roles")
                .body(formInserter).headers(JwtUtil.addJwt(JwtUtil.jwt("sonam")))
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();
        LOG.info("response: {}", entityExchangeResult.getResponseBody());

        // take request for mocked response of access token
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/clients/organizations/users/roles");

       takeRequests(mockWebServer, oauthClient, organizationId);
    }


    private void setMockResponse(OauthClient oauthClient, UUID organizationId) {
        RegisteredClient registeredClient = oauthClient.getRegisteredClient();
        Map<String, Object> map = registeredClientUtil.getMapObject(registeredClient);
        String json = getJson(map);
        assertThat(oauthClient.getId()).isNotNull();

        //1 response for getting a client by id
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(json));

        //2  return a UUID for get organizationIdAssociatedWithClientId call
       // UUID organizationId = UUID.randomUUID();
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(organizationId)));

        //3  getOrganizationById

        Organization organization = new Organization(organizationId, "Free Press Organization", UUID.randomUUID());
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(organization)));

        // 4 getRolesByOrganizationId
        json = "[Role{id=a617b9c7-c46a-41cf-97c3-cbeee3c454e7, name='AppleTreeCareTaker', userId=1f442dab-96a3-459e-8605-7f5cd5f82e25, roleOrganization=null}]";
        List<Role> roles = List.of(new Role(UUID.fromString("a617b9c7-c46a-41cf-97c3-cbeee3c454e7"), "AppleTreeCareTaker",organizationId));
        RestPage<Role> restPage = new RestPage<>(roles, 1, 1, 1);

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(restPage)));

        // 5 getUsersInOrganizationId
        //List<User> userList = List.of(new User(UUID.fromString("5eb2eb31-e80c-4924-be00-50a96b12aa3b"), "test6@sonam.email"), new User(UUID.fromString("1f442dab-96a3-459e-8605-7f5cd5f82e25"), "tom@tom.com"));
        List<UUID> userIds = List.of(UUID.fromString("1f442dab-96a3-459e-8605-7f5cd5f82e25"), UUID.fromString("5eb2eb31-e80c-4924-be00-50a96b12aa3b"));
        RestPage<UUID> usersInOrg = new RestPage<>(userIds, 1,1,2);

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(usersInOrg)));

        List<me.sonam.authzmanager.clients.user.User> userList = List.of(new me.sonam.authzmanager.clients.user.User(UUID.fromString("1f442dab-96a3-459e-8605-7f5cd5f82e25")),
                new me.sonam.authzmanager.clients.user.User(UUID.fromString("5eb2eb31-e80c-4924-be00-50a96b12aa3b")));
        //6 getUserByBatchOfIds
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(userList)));

        Role role = new Role(UUID.fromString("a617b9c7-c46a-41cf-97c3-cbeee3c454e7"), "AppleCareTaker", organizationId);
        User user = new User(UUID.fromString("1f442dab-96a3-459e-8605-7f5cd5f82e25"), role);
        List<ClientOrganizationUserWithRole> clientOrganizations = List.of(
                new ClientOrganizationUserWithRole(UUID.fromString("f8b36547-9f1e-4905-b726-e50e76a9076b"),
                       organizationId, user, role));

        //getClientOrganizationUserWithRoles
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(clientOrganizations)));
        //"{\"clientId\":\"f8b36547-9f1e-4905-b726-e50e76a9076b\", \"organizationId\":\"18a528d0-8686-4ecc-ae7e-fba9a8654f5b\", " +
        //        "\"user\":\"User\"{\"id\":\"1f442dab-96a3-459e-8605-7f5cd5f82e25\", \"firstName\":\"null\", \"lastName\":\"null\", \"email='null', authenticationId='null', role=Role{id=a617b9c7-c46a-41cf-97c3-cbeee3c454e7, name='AppleTreeCareTaker', userId=1f442dab-96a3-459e-8605-7f5cd5f82e25, roleOrganization=null}}}, ClientOrganziationUserWithRole{clientId=f8b36547-9f1e-4905-b726-e50e76a9076b, organizationId=18a528d0-8686-4ecc-ae7e-fba9a8654f5b, user=User{id=5eb2eb31-e80c-4924-be00-50a96b12aa3b, firstName='null', lastName='null', email='null', authenticationId='null', role=Role{id=a617b9c7-c46a-41cf-97c3-cbeee3c454e7, name='AppleTreeCareTaker', userId=1f442dab-96a3-459e-8605-7f5cd5f82e25, roleOrganization=null}}}]

    }
    public void takeRequests(MockWebServer mockWebServer, OauthClient oauthClient, UUID organizationId) throws InterruptedException {

        // take request for mocked response of access token
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/issuer/clients");
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");

        //2
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/issuer/clients/"+oauthClient.getId()+"/organizations/id");
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");

        //3
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/");
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");

        //4
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/organizations/");
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");

        //5
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/");
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");

        //6
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/users/ids/");
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");

        //7
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/clients/"+oauthClient.getId()
                + "/organizations/"+organizationId+"/users/roles");
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("PUT");
    }

    private String getJson(Object object) {
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
    private OauthClient getOauthClient() {
        OauthClient oauthClient = new OauthClient();
        LOG.info("setting oauthClient");
        oauthClient.setClientIdUuid(UUID.randomUUID());
        oauthClient.setClientId("cloud.sonam.app.test");
        oauthClient.setClientSecret("secret");

        oauthClient.setClientAuthenticationMethods(List.of("CLIENT_SECRET_BASIC"));

        oauthClient.setAuthorizationGrantTypes( List.of("CLIENT_CREDENTIALS"));

        List<String> scopes = List.of(OidcScopes.OPENID.toUpperCase(), OidcScopes.PHONE.toUpperCase());
        oauthClient.setScopes(scopes);

        oauthClient.setRedirectUris("http://www.sonam.cloud");
        ClientSettings.Builder clientSettings1Builder =
                ClientSettings.builder();
        clientSettings1Builder.requireAuthorizationConsent(true);
        clientSettings1Builder.requireProofKey(true);

        OauthClient.ClientSettings clientSettings = new OauthClient.ClientSettings();
        clientSettings.setRequireProofKey(true);
        clientSettings.setRequireAuthorizationConsent(true);

        oauthClient.setClientSettings(clientSettings);

        oauthClient.setTokenSettings(null);
        oauthClient.setClientSettings(null);
        LOG.info("calling  post method");

        return oauthClient;
    }
}