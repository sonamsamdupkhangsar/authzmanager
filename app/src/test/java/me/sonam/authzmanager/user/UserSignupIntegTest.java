package me.sonam.authzmanager.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.sonam.authzmanager.Application;
import me.sonam.authzmanager.clients.user.User;
import me.sonam.authzmanager.controller.admin.clients.ClientController;
import me.sonam.authzmanager.controller.admin.organization.Organization;
import me.sonam.authzmanager.controller.signup.UserSignup;
import me.sonam.authzmanager.oauth2.util.RegisteredClientUtil;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@AutoConfigureWebTestClient
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
    @MockitoBean
    ReactiveJwtDecoder jwtDecoder;
    private static MockWebServer mockWebServer;

    private RegisteredClientUtil registeredClientUtil = new RegisteredClientUtil();
    private final String token = "{\"access_token\": \"eyJraWQiOiJlOGQ3MjIzMC1iMDgwLTRhZjEtODFkOC0zMzE3NmNhMTM5ODIiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJhdWQiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJuYmYiOjE3MTQ3NTY2ODIsImlzcyI6Imh0dHA6Ly9teS1zZXJ2ZXI6OTAwMSIsImV4cCI6MTcxNDc1Njk4MiwiaWF0IjoxNzE0NzU2NjgyLCJqdGkiOiI0NDBlZDY0My00MzdkLTRjOTMtYTZkMi1jNzYxNjFlNDRlZjUifQ.fjqgoczZbbmcnvYpVN4yakpbplp7EkDyxslvar5nXBFa6mgIFcZa29fwIKfcie3oUMQ8MDWxayak5PZ_QIuHwTvKSWHs0WL91ljf-GT1sPi1b4gDKf0rJOwi0ClcoTCRIx9-WGR6t2BBR1Rk6RGF2MW7xKw8M-RMac2A2mPEPJqoh4Pky1KgxhZpEXixegpAdQIvBgc0KBZeQme-ZzTYugB8EPUmGpMlfd-zX_vcR1ijxi8e-LRRJMqmGkc9GXfrH7MOKNQ_nu6pc6Gish2v_iuUEcpPHXrfqzGb9IHCLvfuLSaTDcYKYjQaEUAp-1uDW8-5posjiUV2eBiU48ajYg\", \"token_type\":\"Bearer\", \"expires_in\":\"299\"}";


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

        OAuth2AccessToken oAuth2AccessToken = mock(OAuth2AccessToken.class);

        when(tokenService.getAccessToken(any())).thenReturn(oAuth2AccessToken);
        when( oAuth2AccessToken.getTokenValue()).thenReturn("sonamstoken");
        when(tokenService.getAccessToken()).thenReturn("dummytoken");

        LOG.info("setup for token mock");
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
    @ParameterizedTest(name = "admin signup passes request subdomain {0}")
    @ValueSource(strings = {
            "business1.admin.openissuer.test",
            "business2.admin.openissuer.test",
            "localhost"
    })
    public void adminSignupChecksEachRequestSubdomain(String subdomain) throws Exception {
        String organizationHost = organizationHostFor(subdomain);
        String email = allowedEmailFor(subdomain, "tenantuser");
        String authenticationId = "tenant-user";
        Jwt jwt = JwtUtil.jwt(authenticationId);

        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        UUID orgId = UUID.randomUUID();
        UUID loggedInUserId = UUID.fromString("5d8de63a-0b45-4c33-b9eb-d7fb8d662107");
        Organization organization = new Organization(orgId, "tenant company", UUID.randomUUID());
        User signedUpUser = new User();
        signedUpUser.setFirstName("Tenant");
        signedUpUser.setLastName("User");
        signedUpUser.setId(UUID.randomUUID());
        signedUpUser.setAuthenticationId(authenticationId);

        enqueueAdminSignupSetup(organization, loggedInUserId, true);
        mockWebServer.enqueue(jsonResponse(Map.of("message", true)));
        mockWebServer.enqueue(jsonResponse(Map.of("error", "user not found")).setResponseCode(404));
        mockWebServer.enqueue(jsonResponse(Map.of("message", "user added successfully")));
        mockWebServer.enqueue(jsonResponse(signedUpUser));
        mockWebServer.enqueue(jsonResponse(Map.of("message", "added user to organization")));
        mockWebServer.enqueue(jsonResponse(Map.of("message", "default organization updated")));

        EntityExchangeResult<String> entityExchangeResult = webTestClient.post()
                .uri("/admin/organizations/users")
                .header(HttpHeaders.HOST, subdomain)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(signupFormData(orgId, "Tenant", "User", email, false))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult();

        Assertions.assertThat(entityExchangeResult.getResponseBody()).contains("Tenant User has been added successfully");

        assertAdminSignupSetupRequests(loggedInUserId, orgId);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath())
                .startsWith("/organizations/subdomain/" + organizationHost + "/organizations/" + orgId + "/can-add-user");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/users/authentication-id/" + email);

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/users");
        Assertions.assertThat(recordedRequest.getBody().readUtf8())
                .contains("\"activationHost\":\"" + organizationHost + "\"");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/users/authentication-id/" + email);

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/users");
        Assertions.assertThat(recordedRequest.getBody().readUtf8())
                .contains("\"subdomain\":\"" + organizationHost + "\"")
                .contains("\"restrictToSubdomain\":true");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("PUT");
        Assertions.assertThat(recordedRequest.getPath())
                .startsWith("/organizations/" + orgId + "/users/" + signedUpUser.getId() + "/default");
    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void adminSignupChecksExistingUserAgainstSubdomainBeforeSignup() throws Exception {
        String subdomain = "business1.admin.openissuer.test";
        String organizationHost = organizationHostFor(subdomain);
        String authenticationId = "existing-user@business1.com";
        Jwt jwt = JwtUtil.jwt(authenticationId);

        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        UUID orgId = UUID.randomUUID();
        UUID loggedInUserId = UUID.fromString("5d8de63a-0b45-4c33-b9eb-d7fb8d662107");
        Organization organization = new Organization(orgId, "tenant company", UUID.randomUUID());
        User existingUser = new User();
        existingUser.setId(UUID.randomUUID());
        existingUser.setAuthenticationId(authenticationId);
        existingUser.setFirstName("Existing");
        existingUser.setLastName("User");

        enqueueAdminSignupSetup(organization, loggedInUserId, true);
        mockWebServer.enqueue(jsonResponse(Map.of("message", true)));
        mockWebServer.enqueue(jsonResponse(existingUser));
        mockWebServer.enqueue(jsonResponse(Map.of("message", true)));
        mockWebServer.enqueue(jsonResponse(Map.of("message", "user added successfully")));
        mockWebServer.enqueue(jsonResponse(existingUser));
        mockWebServer.enqueue(jsonResponse(Map.of("message", "added user to organization")));
        mockWebServer.enqueue(jsonResponse(Map.of("message", "default organization updated")));

        webTestClient.post()
                .uri("/admin/organizations/users")
                .header(HttpHeaders.HOST, subdomain)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(signupFormData(orgId, "Existing", "User", authenticationId, true))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> Assertions.assertThat(body)
                        .contains("Existing User has been added successfully")
                        .contains("Their account is now active"));

        assertAdminSignupSetupRequests(loggedInUserId, orgId);
        assertOrganizationSubdomainPreflight(organizationHost, orgId);
        assertUserLookup(authenticationId);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath())
                .startsWith("/organizations/subdomain/" + organizationHost + "/users/" + existingUser.getId()
                        + "/organizations/" + orgId + "/can-add");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/users");

        assertUserLookup(authenticationId);

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/users");
        Assertions.assertThat(recordedRequest.getBody().readUtf8())
                .contains("\"subdomain\":\"" + organizationHost + "\"")
                .contains("\"restrictToSubdomain\":true");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("PUT");
        Assertions.assertThat(recordedRequest.getPath())
                .startsWith("/organizations/" + orgId + "/users/" + existingUser.getId() + "/default");
    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void adminSignupWithoutInitialPasswordDoesNotSubmitBlankPassword() throws Exception {
        String subdomain = "business1.admin.openissuer.test";
        String organizationHost = organizationHostFor(subdomain);
        String email = "nopassword-user@business1.com";
        Jwt jwt = JwtUtil.jwt(email);

        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        UUID orgId = UUID.randomUUID();
        UUID loggedInUserId = UUID.fromString("5d8de63a-0b45-4c33-b9eb-d7fb8d662107");
        Organization organization = new Organization(orgId, "tenant company", UUID.randomUUID());
        User signedUpUser = new User();
        signedUpUser.setId(UUID.randomUUID());
        signedUpUser.setAuthenticationId(email);
        signedUpUser.setFirstName("Noah");
        signedUpUser.setLastName("Password");

        enqueueAdminSignupSetup(organization, loggedInUserId, true);
        mockWebServer.enqueue(jsonResponse(Map.of("message", true)));
        mockWebServer.enqueue(jsonResponse(Map.of("error", "user not found")).setResponseCode(404));
        mockWebServer.enqueue(jsonResponse(Map.of("message", "user added successfully")));
        mockWebServer.enqueue(jsonResponse(signedUpUser));
        mockWebServer.enqueue(jsonResponse(Map.of("message", "added user to organization")));
        mockWebServer.enqueue(jsonResponse(Map.of("message", "default organization updated")));

        webTestClient.post()
                .uri("/admin/organizations/users")
                .header(HttpHeaders.HOST, subdomain)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(signupFormDataWithoutPassword(orgId, "Noah", "Password", email, false))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> Assertions.assertThat(body)
                        .contains("Noah Password has been added successfully")
                        .doesNotContain("Failed to add user"));

        assertAdminSignupSetupRequests(loggedInUserId, orgId);
        assertOrganizationSubdomainPreflight(organizationHost, orgId);
        assertUserLookup(email);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/users");
        Assertions.assertThat(recordedRequest.getBody().readUtf8()).doesNotContain("\"password\":\"\"");

        assertUserLookup(email);

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/users");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("PUT");
        Assertions.assertThat(recordedRequest.getPath())
                .startsWith("/organizations/" + orgId + "/users/" + signedUpUser.getId() + "/default");
    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void adminSignupShowsErrorWhenOrganizationRejectsSubdomain() throws Exception {
        String subdomain = "blocked.admin.openissuer.test";
        String organizationHost = organizationHostFor(subdomain);
        UUID orgId = UUID.randomUUID();
        UUID loggedInUserId = UUID.fromString("5d8de63a-0b45-4c33-b9eb-d7fb8d662107");
        Organization organization = new Organization(orgId, "blocked company", UUID.randomUUID());

        enqueueAdminSignupSetup(organization, loggedInUserId, true);
        mockWebServer.enqueue(jsonResponse(Map.of("message", false, "reason", "subdomain is not allowed")));

        webTestClient.post()
                .uri("/admin/organizations/users")
                .header(HttpHeaders.HOST, subdomain)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(signupFormData(orgId, "Blocked", "User", "blocked-user@sonam.cloud", false))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> Assertions.assertThat(body).contains("subdomain is not allowed"));

        assertAdminSignupSetupRequests(loggedInUserId, orgId);
        assertOrganizationSubdomainPreflight(organizationHost, orgId);
        Assertions.assertThat(mockWebServer.takeRequest(100, TimeUnit.MILLISECONDS)).isNull();
    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void adminSignupShowsErrorWhenExistingUserCannotJoinSubdomainOrganization() throws Exception {
        String subdomain = "business2.admin.openissuer.test";
        String organizationHost = organizationHostFor(subdomain);
        String authenticationId = "cross-tenant-user@business2.com";
        UUID orgId = UUID.randomUUID();
        UUID loggedInUserId = UUID.fromString("5d8de63a-0b45-4c33-b9eb-d7fb8d662107");
        Organization organization = new Organization(orgId, "tenant company", UUID.randomUUID());
        User existingUser = new User();
        existingUser.setId(UUID.randomUUID());
        existingUser.setAuthenticationId(authenticationId);

        enqueueAdminSignupSetup(organization, loggedInUserId, true);
        mockWebServer.enqueue(jsonResponse(Map.of("message", true)));
        mockWebServer.enqueue(jsonResponse(existingUser));
        mockWebServer.enqueue(jsonResponse(Map.of("message", false, "reason", "user belongs to another subdomain")));

        webTestClient.post()
                .uri("/admin/organizations/users")
                .header(HttpHeaders.HOST, subdomain)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(signupFormData(orgId, "Cross", "Tenant", authenticationId, false))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> Assertions.assertThat(body).contains("user belongs to another subdomain"));

        assertAdminSignupSetupRequests(loggedInUserId, orgId);
        assertOrganizationSubdomainPreflight(organizationHost, orgId);
        assertUserLookup(authenticationId);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath())
                .startsWith("/organizations/subdomain/" + organizationHost + "/users/" + existingUser.getId()
                        + "/organizations/" + orgId + "/can-add");
        Assertions.assertThat(mockWebServer.takeRequest(100, TimeUnit.MILLISECONDS)).isNull();
    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void adminSignupRejectsEmailOutsideTenantDomain() throws Exception {
        String subdomain = "business1.admin.openissuer.test";
        UUID orgId = UUID.randomUUID();
        UUID loggedInUserId = UUID.fromString("5d8de63a-0b45-4c33-b9eb-d7fb8d662107");
        Organization organization = new Organization(orgId, "tenant company", UUID.randomUUID());

        enqueueAdminSignupSetup(organization, loggedInUserId, true);

        webTestClient.post()
                .uri("/admin/organizations/users")
                .header(HttpHeaders.HOST, subdomain)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(signupFormData(orgId, "Wrong", "Domain", "wrong-domain@example.com", false))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> Assertions.assertThat(body)
                        .contains("user email domain is not allowed for this business"));

        assertAdminSignupSetupRequests(loggedInUserId, orgId);
        Assertions.assertThat(mockWebServer.takeRequest(100, TimeUnit.MILLISECONDS)).isNull();
    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void admInUserSignup() throws Exception {
        LOG.info("signup user by admin");
        String authenticationId = "dave";
        Jwt jwt = JwtUtil.jwt(authenticationId);

        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));
        UserSignup userSignup = new UserSignup("Sonam", "Wangyal", "mugambo@1234sonam.com", "mugambo", "hello".toCharArray(), false, "myOrganization");

        Map<String, Object> map = new HashMap<>();
        map.put("firstName", "Sonam");
        map.put("lastName", "Wangyal");
        map.put("email", "SOnAm@123zuma.com");
        map.put("authenticationId", "soNAM");
        map.put("password", "12345");
        map.put("active", false);

        UUID orgId = UUID.randomUUID();

        UUID userId = UUID.fromString("5d8de63a-0b45-4c33-b9eb-d7fb8d662107");

        Organization organization = new Organization(orgId, "my company", UUID.randomUUID());

        //1 get default organization
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(Map.of("message", organization.getId()))));

        //2 superAdmin check response
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(Map.of("message", true))));

        //2 get organization by id response
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(organization)));

        //
        User user = new User();
        user.setFirstName(map.get("firstName").toString());
        user.setLastName(map.get("lastName").toString());
        user.setId(UUID.randomUUID());
        user.setAuthenticationId(map.get("authenticationId").toString());

        //3 selected organization can accept users from this subdomain
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(Map.of("message", true))));

        //4 user does not exist yet for user-specific preflight
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(404).setBody(getJson(Map.of("error", "user not found"))));

        //5 user signup response
       mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody("{\"message\": \"user added successfully\"}"));

       //6 find user by auth-id response
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(user)));

        //7 add user to org response
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody("{\"message\": \"added user to organization\"}"));

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody("{\"message\": \"default organization updated\"}"));


        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("firstName", userSignup.getFirstName());
        formData.add("lastName", userSignup.getLastName());
        formData.add("email", userSignup.getEmail());
        formData.add("authenticationId", userSignup.getAuthenticationId());
        formData.add("password", "1234567890");
        formData.add("organizationId", orgId.toString());
        formData.add("active", "false");
        EntityExchangeResult<String> entityExchangeResult = webTestClient.post()
                .uri("/admin/organizations/users")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();
        LOG.info("response: {}", entityExchangeResult.getResponseBody());


        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        // get default org take
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/subdomain/");
        Assertions.assertThat(recordedRequest.getPath()).contains("/users/" + userId + "/default-organization-id");

        //is user superadmin in org take
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/authzmanagerroles/users/"+userId+"/organizations/"+organization.getId());

        //get org by id take
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/"+orgId);

        //preflight organization can accept user from subdomain
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/subdomain/");
        Assertions.assertThat(recordedRequest.getPath()).contains("/organizations/" + orgId + "/can-add-user");

        //preflight existing user lookup
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/users/authentication-id/"+userSignup.getAuthenticationId());

        //signup user take
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/users");

        //find user by auth id take
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/users/authentication-id/"+userSignup.getAuthenticationId());

        //add user to org take
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/users");

        //set default organization for user
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("PUT");
        Assertions.assertThat(recordedRequest.getPath())
                .startsWith("/organizations/" + orgId + "/users/" + user.getId() + "/default");

        LOG.info("done signing up user by admin");
        LOG.info("mvcResult: {}", entityExchangeResult.getResponseBody());
    }


    public String getJson(Object o) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(o);
    }

    private void enqueueAdminSignupSetup(Organization organization, UUID loggedInUserId, boolean superAdmin)
            throws JsonProcessingException {
        mockWebServer.enqueue(jsonResponse(Map.of("message", organization.getId())));
        mockWebServer.enqueue(jsonResponse(Map.of("message", superAdmin)));
        mockWebServer.enqueue(jsonResponse(organization));
    }

    private void assertAdminSignupSetupRequests(UUID loggedInUserId, UUID organizationId) throws InterruptedException {
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/subdomain/");
        Assertions.assertThat(recordedRequest.getPath()).contains("/users/" + loggedInUserId + "/default-organization-id");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath())
                .startsWith("/roles/authzmanagerroles/users/" + loggedInUserId + "/organizations/" + organizationId);

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/" + organizationId);
    }

    private void assertOrganizationSubdomainPreflight(String subdomain, UUID organizationId) throws InterruptedException {
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath())
                .startsWith("/organizations/subdomain/" + subdomain + "/organizations/" + organizationId + "/can-add-user");
    }

    private String organizationHostFor(String requestHost) {
        if (requestHost.contains(".admin.")) {
            return requestHost.replace(".admin.", ".");
        }
        return "openissuer.test";
    }

    private String allowedEmailFor(String requestHost, String username) {
        if (requestHost.startsWith("business1.")) {
            return username + "@business1.com";
        }
        if (requestHost.startsWith("business2.")) {
            return username + "@business2.com";
        }
        return username + "@sonam.cloud";
    }

    private void assertUserLookup(String authenticationId) throws InterruptedException {
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/users/authentication-id/" + authenticationId);
    }

    private MultiValueMap<String, String> signupFormData(UUID organizationId, String firstName, String lastName,
                                                         String email, boolean active) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("firstName", firstName);
        formData.add("lastName", lastName);
        formData.add("email", email);
        formData.add("authenticationId", email);
        formData.add("password", "1234567890");
        formData.add("organizationId", organizationId.toString());
        formData.add("active", Boolean.toString(active));
        return formData;
    }

    private MultiValueMap<String, String> signupFormDataWithoutPassword(UUID organizationId, String firstName,
                                                                        String lastName, String email,
                                                                        boolean active) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("firstName", firstName);
        formData.add("lastName", lastName);
        formData.add("email", email);
        formData.add("authenticationId", email);
        formData.add("organizationId", organizationId.toString());
        formData.add("active", Boolean.toString(active));
        return formData;
    }

    private MockResponse jsonResponse(Object object) throws JsonProcessingException {
        return new MockResponse()
                .setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200)
                .setBody(getJson(object));
    }
}
