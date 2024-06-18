package me.sonam.authzmanager.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.sonam.authzmanager.Application;
import me.sonam.authzmanager.clients.user.ClientOrganization;
import me.sonam.authzmanager.clients.user.User;
import me.sonam.authzmanager.oauth2.OauthClient;
import me.sonam.authzmanager.oauth2.OidcScopes;
import me.sonam.authzmanager.oauth2.ClientSettings;
import me.sonam.authzmanager.oauth2.RegisteredClient;
import me.sonam.authzmanager.oauth2.util.RegisteredClientUtil;
import me.sonam.authzmanager.controller.admin.organization.Organization;
import me.sonam.authzmanager.controller.admin.roles.Role;
import me.sonam.authzmanager.controller.admin.clients.ClientController;
import me.sonam.authzmanager.controller.admin.clients.carrier.ClientOrganizationUserWithRole;
import me.sonam.authzmanager.controller.util.MyPair;
import me.sonam.authzmanager.rest.RestPage;
import me.sonam.authzmanager.security.WithMockCustomUser;
import me.sonam.authzmanager.tokenfilter.TokenService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.validation.support.BindingAwareModelMap;
import org.springframework.web.reactive.function.BodyInserters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Application.class})
public class ClientControllerIntegTest {
    private static final Logger LOG = LoggerFactory.getLogger(ClientControllerIntegTest.class);

    @Autowired
    private ClientController clientController;
    private String userId = UUID.randomUUID().toString();

    @Autowired
    private WebTestClient webTestClient;

    @Mock
    private TokenService tokenService;

    @Autowired
    private MockMvc mockMvc;
    private static MockWebServer mockWebServer;

    private RegisteredClientUtil registeredClientUtil = new RegisteredClientUtil();
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
        r.add("user-rest-service.root", () -> "http://localhost:" + mockWebServer.getPort());
    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void getCreateForm() {
        Model model = new BindingAwareModelMap();
        String view = clientController.getCreateForm(model);
        assertThat(view).isEqualTo("admin/clients/form");
        OauthClient oauthClient = (OauthClient) model.getAttribute("client");
        assertThat(oauthClient).isNotNull();
        assertThat(oauthClient.getId()).isNull();
        assertThat(oauthClient.getClientIdUuid()).isNotNull(); //the form controller should set the clientIdUUid to append with clientId
    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void createClient() throws Exception {
        final String clientId = saveOauthClient();
    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void getClientByClientId() throws InterruptedException {
        LOG.info("get client by client's id");

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setResponseCode(200).setBody("{\"access_token\": \"eyJraWQiOiJlOGQ3MjIzMC1iMDgwLTRhZjEtODFkOC0zMzE3NmNhMTM5ODIiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJhdWQiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJuYmYiOjE3MTQ3NTY2ODIsImlzcyI6Imh0dHA6Ly9teS1zZXJ2ZXI6OTAwMSIsImV4cCI6MTcxNDc1Njk4MiwiaWF0IjoxNzE0NzU2NjgyLCJqdGkiOiI0NDBlZDY0My00MzdkLTRjOTMtYTZkMi1jNzYxNjFlNDRlZjUifQ.fjqgoczZbbmcnvYpVN4yakpbplp7EkDyxslvar5nXBFa6mgIFcZa29fwIKfcie3oUMQ8MDWxayak5PZ_QIuHwTvKSWHs0WL91ljf-GT1sPi1b4gDKf0rJOwi0ClcoTCRIx9-WGR6t2BBR1Rk6RGF2MW7xKw8M-RMac2A2mPEPJqoh4Pky1KgxhZpEXixegpAdQIvBgc0KBZeQme-ZzTYugB8EPUmGpMlfd-zX_vcR1ijxi8e-LRRJMqmGkc9GXfrH7MOKNQ_nu6pc6Gish2v_iuUEcpPHXrfqzGb9IHCLvfuLSaTDcYKYjQaEUAp-1uDW8-5posjiUV2eBiU48ajYg\", \"token_type\":\"Bearer\", \"expires_in\":\"299\"}"));

        OauthClient oauthClient = getOauthClient();

        oauthClient.setId(UUID.randomUUID().toString());
        RegisteredClient registeredClient = oauthClient.getRegisteredClient();
        Map<String, Object> map = registeredClientUtil.getMapObject(registeredClient);
        String json = getJson(map);

        LOG.info("setting json as response for client rest service: {}", json);

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(json));

        when(tokenService.getAccessToken()).thenReturn("sometokenvalue");

        EntityExchangeResult<String> entityExchangeResult = webTestClient.get()
                .uri("/admin/clients/"+oauthClient.getId())
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();
        LOG.info("response: {}", entityExchangeResult.getResponseBody());

        // take request for mocked response of access token
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/oauth2/token");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/issuer/clients");
    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void getLoggedInUserClients() throws InterruptedException {
        LOG.info("get client by client's id");

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setResponseCode(200).setBody("{\"access_token\": \"eyJraWQiOiJlOGQ3MjIzMC1iMDgwLTRhZjEtODFkOC0zMzE3NmNhMTM5ODIiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJhdWQiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJuYmYiOjE3MTQ3NTY2ODIsImlzcyI6Imh0dHA6Ly9teS1zZXJ2ZXI6OTAwMSIsImV4cCI6MTcxNDc1Njk4MiwiaWF0IjoxNzE0NzU2NjgyLCJqdGkiOiI0NDBlZDY0My00MzdkLTRjOTMtYTZkMi1jNzYxNjFlNDRlZjUifQ.fjqgoczZbbmcnvYpVN4yakpbplp7EkDyxslvar5nXBFa6mgIFcZa29fwIKfcie3oUMQ8MDWxayak5PZ_QIuHwTvKSWHs0WL91ljf-GT1sPi1b4gDKf0rJOwi0ClcoTCRIx9-WGR6t2BBR1Rk6RGF2MW7xKw8M-RMac2A2mPEPJqoh4Pky1KgxhZpEXixegpAdQIvBgc0KBZeQme-ZzTYugB8EPUmGpMlfd-zX_vcR1ijxi8e-LRRJMqmGkc9GXfrH7MOKNQ_nu6pc6Gish2v_iuUEcpPHXrfqzGb9IHCLvfuLSaTDcYKYjQaEUAp-1uDW8-5posjiUV2eBiU48ajYg\", \"token_type\":\"Bearer\", \"expires_in\":\"299\"}"));

        List<MyPair<String, String>> clientIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            clientIds.add(new MyPair<>(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        }

        String json = getJson(clientIds);

        LOG.info("setting json as response for client rest service: {}", json);

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(json));

        EntityExchangeResult<String> entityExchangeResult = webTestClient.get()
                .uri("/admin/clients")
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();
        LOG.info("response: {}", entityExchangeResult.getResponseBody());

        // take request for mocked response of access token
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/oauth2/token");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/issuer/clients");
    }

    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void deleteClientBydId() throws InterruptedException {
        LOG.info("get client by client's id");

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setResponseCode(200).setBody("{\"access_token\": \"eyJraWQiOiJlOGQ3MjIzMC1iMDgwLTRhZjEtODFkOC0zMzE3NmNhMTM5ODIiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJhdWQiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJuYmYiOjE3MTQ3NTY2ODIsImlzcyI6Imh0dHA6Ly9teS1zZXJ2ZXI6OTAwMSIsImV4cCI6MTcxNDc1Njk4MiwiaWF0IjoxNzE0NzU2NjgyLCJqdGkiOiI0NDBlZDY0My00MzdkLTRjOTMtYTZkMi1jNzYxNjFlNDRlZjUifQ.fjqgoczZbbmcnvYpVN4yakpbplp7EkDyxslvar5nXBFa6mgIFcZa29fwIKfcie3oUMQ8MDWxayak5PZ_QIuHwTvKSWHs0WL91ljf-GT1sPi1b4gDKf0rJOwi0ClcoTCRIx9-WGR6t2BBR1Rk6RGF2MW7xKw8M-RMac2A2mPEPJqoh4Pky1KgxhZpEXixegpAdQIvBgc0KBZeQme-ZzTYugB8EPUmGpMlfd-zX_vcR1ijxi8e-LRRJMqmGkc9GXfrH7MOKNQ_nu6pc6Gish2v_iuUEcpPHXrfqzGb9IHCLvfuLSaTDcYKYjQaEUAp-1uDW8-5posjiUV2eBiU48ajYg\", \"token_type\":\"Bearer\", \"expires_in\":\"299\"}"));

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(""));

        UUID clientsId = UUID.randomUUID();
        EntityExchangeResult<String> entityExchangeResult = webTestClient.delete()
                .uri("/admin/clients/"+clientsId)
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();
        LOG.info("response: {}", entityExchangeResult.getResponseBody());

        // take request for mocked response of access token
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/oauth2/token");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("DELETE");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/issuer/clients/"+clientsId);
    }

    /**
     * this gets organizations created by this user, or owner
     * @throws InterruptedException
     */
    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void getUserCreatedOrganizations() throws InterruptedException {
        LOG.info("get client by client's id");

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setResponseCode(200).setBody("{\"access_token\": \"eyJraWQiOiJlOGQ3MjIzMC1iMDgwLTRhZjEtODFkOC0zMzE3NmNhMTM5ODIiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJhdWQiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJuYmYiOjE3MTQ3NTY2ODIsImlzcyI6Imh0dHA6Ly9teS1zZXJ2ZXI6OTAwMSIsImV4cCI6MTcxNDc1Njk4MiwiaWF0IjoxNzE0NzU2NjgyLCJqdGkiOiI0NDBlZDY0My00MzdkLTRjOTMtYTZkMi1jNzYxNjFlNDRlZjUifQ.fjqgoczZbbmcnvYpVN4yakpbplp7EkDyxslvar5nXBFa6mgIFcZa29fwIKfcie3oUMQ8MDWxayak5PZ_QIuHwTvKSWHs0WL91ljf-GT1sPi1b4gDKf0rJOwi0ClcoTCRIx9-WGR6t2BBR1Rk6RGF2MW7xKw8M-RMac2A2mPEPJqoh4Pky1KgxhZpEXixegpAdQIvBgc0KBZeQme-ZzTYugB8EPUmGpMlfd-zX_vcR1ijxi8e-LRRJMqmGkc9GXfrH7MOKNQ_nu6pc6Gish2v_iuUEcpPHXrfqzGb9IHCLvfuLSaTDcYKYjQaEUAp-1uDW8-5posjiUV2eBiU48ajYg\", \"token_type\":\"Bearer\", \"expires_in\":\"299\"}"));

        OauthClient oauthClient = getOauthClient();

        oauthClient.setId(UUID.randomUUID().toString());
        RegisteredClient registeredClient = oauthClient.getRegisteredClient();
        Map<String, Object> map = registeredClientUtil.getMapObject(registeredClient);
        String json = getJson(map);

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(json));

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setResponseCode(200).setBody("{\"access_token\": \"eyJraWQiOiJlOGQ3MjIzMC1iMDgwLTRhZjEtODFkOC0zMzE3NmNhMTM5ODIiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJhdWQiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJuYmYiOjE3MTQ3NTY2ODIsImlzcyI6Imh0dHA6Ly9teS1zZXJ2ZXI6OTAwMSIsImV4cCI6MTcxNDc1Njk4MiwiaWF0IjoxNzE0NzU2NjgyLCJqdGkiOiI0NDBlZDY0My00MzdkLTRjOTMtYTZkMi1jNzYxNjFlNDRlZjUifQ.fjqgoczZbbmcnvYpVN4yakpbplp7EkDyxslvar5nXBFa6mgIFcZa29fwIKfcie3oUMQ8MDWxayak5PZ_QIuHwTvKSWHs0WL91ljf-GT1sPi1b4gDKf0rJOwi0ClcoTCRIx9-WGR6t2BBR1Rk6RGF2MW7xKw8M-RMac2A2mPEPJqoh4Pky1KgxhZpEXixegpAdQIvBgc0KBZeQme-ZzTYugB8EPUmGpMlfd-zX_vcR1ijxi8e-LRRJMqmGkc9GXfrH7MOKNQ_nu6pc6Gish2v_iuUEcpPHXrfqzGb9IHCLvfuLSaTDcYKYjQaEUAp-1uDW8-5posjiUV2eBiU48ajYg\", \"token_type\":\"Bearer\", \"expires_in\":\"299\"}"));

        json = "{\"content\":[{\"id\":\"2cc2cf6e-7adb-4c31-a4c6-906d6b024a1e\",\"name\":\"Apple grower\",\"creatorUserId\":\"1f442dab-96a3-459e-8605-7f5cd5f82e25\"},{\"id\":\"b0420d13-5a46-4e55-b007-f2f4ab0f04f1\",\"name\":\"Cup maker\",\"creatorUserId\":\"1f442dab-96a3-459e-8605-7f5cd5f82e25\"},{\"id\":\"0dced43e-3f93-4a8f-8911-f0aecd5fcdba\",\"name\":\"Dog caretaker\",\"creatorUserId\":\"1f442dab-96a3-459e-8605-7f5cd5f82e25\"},{\"id\":\"0a62b3ae-37fb-4a7d-be32-7a319db9fe26\",\"name\":\"Dog groomer\",\"creatorUserId\":\"1f442dab-96a3-459e-8605-7f5cd5f82e25\"},{\"id\":\"18a528d0-8686-4ecc-ae7e-fba9a8654f5b\",\"name\":\"Farm manager\",\"creatorUserId\":\"1f442dab-96a3-459e-8605-7f5cd5f82e25\"}],\"number\":0,\"size\":5,\"totalElements\":12,\"numberOfElements\":5,\"totalPages\":3,\"last\":false,\"sort\":{\"empty\":true,\"sorted\":false,\"unsorted\":true},\"first\":true,\"empty\":false}";

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(json));

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setResponseCode(200).setBody("{\"access_token\": \"eyJraWQiOiJlOGQ3MjIzMC1iMDgwLTRhZjEtODFkOC0zMzE3NmNhMTM5ODIiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJhdWQiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJuYmYiOjE3MTQ3NTY2ODIsImlzcyI6Imh0dHA6Ly9teS1zZXJ2ZXI6OTAwMSIsImV4cCI6MTcxNDc1Njk4MiwiaWF0IjoxNzE0NzU2NjgyLCJqdGkiOiI0NDBlZDY0My00MzdkLTRjOTMtYTZkMi1jNzYxNjFlNDRlZjUifQ.fjqgoczZbbmcnvYpVN4yakpbplp7EkDyxslvar5nXBFa6mgIFcZa29fwIKfcie3oUMQ8MDWxayak5PZ_QIuHwTvKSWHs0WL91ljf-GT1sPi1b4gDKf0rJOwi0ClcoTCRIx9-WGR6t2BBR1Rk6RGF2MW7xKw8M-RMac2A2mPEPJqoh4Pky1KgxhZpEXixegpAdQIvBgc0KBZeQme-ZzTYugB8EPUmGpMlfd-zX_vcR1ijxi8e-LRRJMqmGkc9GXfrH7MOKNQ_nu6pc6Gish2v_iuUEcpPHXrfqzGb9IHCLvfuLSaTDcYKYjQaEUAp-1uDW8-5posjiUV2eBiU48ajYg\", \"token_type\":\"Bearer\", \"expires_in\":\"299\"}"));

        ClientOrganization clientOrganization = new ClientOrganization(UUID.fromString(oauthClient.getId()), UUID.fromString("2cc2cf6e-7adb-4c31-a4c6-906d6b024a1e"));
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(clientOrganization)));

        UUID clientsId = UUID.randomUUID();

        EntityExchangeResult<String> entityExchangeResult = webTestClient.get()
                .uri("/admin/clients/"+clientsId+"/organizations")
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();
        LOG.info("response: {}", entityExchangeResult.getResponseBody());

        // take request for mocked response of access token
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/oauth2/token");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/issuer/clients");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/oauth2/token");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/owner");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/oauth2/token");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("PUT");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/issuer/clients/organizations");
    }


    /**
     * this is for running thru the controller for getting users in organization and assigned to the client with a role
     * @throws InterruptedException
     */
    @WithMockCustomUser(userId = "5d8de63a-0b45-4c33-b9eb-d7fb8d662107", username = "user@sonam.cloud", password = "password", role = "ROLE_USER")
    @Test
    public void getUsersInOrganizationAndAssignedToClient() throws InterruptedException {
        LOG.info("get client by client's id");

        setUsersAndsersInClientOrganizationUserRole(mockWebServer);
    }

    public void setUsersAndsersInClientOrganizationUserRole(MockWebServer mockWebServer) throws InterruptedException {

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setResponseCode(200).setBody("{\"access_token\": \"eyJraWQiOiJlOGQ3MjIzMC1iMDgwLTRhZjEtODFkOC0zMzE3NmNhMTM5ODIiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJhdWQiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJuYmYiOjE3MTQ3NTY2ODIsImlzcyI6Imh0dHA6Ly9teS1zZXJ2ZXI6OTAwMSIsImV4cCI6MTcxNDc1Njk4MiwiaWF0IjoxNzE0NzU2NjgyLCJqdGkiOiI0NDBlZDY0My00MzdkLTRjOTMtYTZkMi1jNzYxNjFlNDRlZjUifQ.fjqgoczZbbmcnvYpVN4yakpbplp7EkDyxslvar5nXBFa6mgIFcZa29fwIKfcie3oUMQ8MDWxayak5PZ_QIuHwTvKSWHs0WL91ljf-GT1sPi1b4gDKf0rJOwi0ClcoTCRIx9-WGR6t2BBR1Rk6RGF2MW7xKw8M-RMac2A2mPEPJqoh4Pky1KgxhZpEXixegpAdQIvBgc0KBZeQme-ZzTYugB8EPUmGpMlfd-zX_vcR1ijxi8e-LRRJMqmGkc9GXfrH7MOKNQ_nu6pc6Gish2v_iuUEcpPHXrfqzGb9IHCLvfuLSaTDcYKYjQaEUAp-1uDW8-5posjiUV2eBiU48ajYg\", \"token_type\":\"Bearer\", \"expires_in\":\"299\"}"));

        OauthClient oauthClient = getOauthClient();

        oauthClient.setId(UUID.randomUUID().toString());
        RegisteredClient registeredClient = oauthClient.getRegisteredClient();
        Map<String, Object> map = registeredClientUtil.getMapObject(registeredClient);
        String json = getJson(map);

        //1 response for getting a client by id
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(json));

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setResponseCode(200).setBody("{\"access_token\": \"eyJraWQiOiJlOGQ3MjIzMC1iMDgwLTRhZjEtODFkOC0zMzE3NmNhMTM5ODIiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJhdWQiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJuYmYiOjE3MTQ3NTY2ODIsImlzcyI6Imh0dHA6Ly9teS1zZXJ2ZXI6OTAwMSIsImV4cCI6MTcxNDc1Njk4MiwiaWF0IjoxNzE0NzU2NjgyLCJqdGkiOiI0NDBlZDY0My00MzdkLTRjOTMtYTZkMi1jNzYxNjFlNDRlZjUifQ.fjqgoczZbbmcnvYpVN4yakpbplp7EkDyxslvar5nXBFa6mgIFcZa29fwIKfcie3oUMQ8MDWxayak5PZ_QIuHwTvKSWHs0WL91ljf-GT1sPi1b4gDKf0rJOwi0ClcoTCRIx9-WGR6t2BBR1Rk6RGF2MW7xKw8M-RMac2A2mPEPJqoh4Pky1KgxhZpEXixegpAdQIvBgc0KBZeQme-ZzTYugB8EPUmGpMlfd-zX_vcR1ijxi8e-LRRJMqmGkc9GXfrH7MOKNQ_nu6pc6Gish2v_iuUEcpPHXrfqzGb9IHCLvfuLSaTDcYKYjQaEUAp-1uDW8-5posjiUV2eBiU48ajYg\", \"token_type\":\"Bearer\", \"expires_in\":\"299\"}"));

        //2  return a UUID for get organizationIdAssociatedWithClientId call
        UUID organizationId = UUID.randomUUID();
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(organizationId)));

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setResponseCode(200).setBody("{\"access_token\": \"eyJraWQiOiJlOGQ3MjIzMC1iMDgwLTRhZjEtODFkOC0zMzE3NmNhMTM5ODIiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJhdWQiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJuYmYiOjE3MTQ3NTY2ODIsImlzcyI6Imh0dHA6Ly9teS1zZXJ2ZXI6OTAwMSIsImV4cCI6MTcxNDc1Njk4MiwiaWF0IjoxNzE0NzU2NjgyLCJqdGkiOiI0NDBlZDY0My00MzdkLTRjOTMtYTZkMi1jNzYxNjFlNDRlZjUifQ.fjqgoczZbbmcnvYpVN4yakpbplp7EkDyxslvar5nXBFa6mgIFcZa29fwIKfcie3oUMQ8MDWxayak5PZ_QIuHwTvKSWHs0WL91ljf-GT1sPi1b4gDKf0rJOwi0ClcoTCRIx9-WGR6t2BBR1Rk6RGF2MW7xKw8M-RMac2A2mPEPJqoh4Pky1KgxhZpEXixegpAdQIvBgc0KBZeQme-ZzTYugB8EPUmGpMlfd-zX_vcR1ijxi8e-LRRJMqmGkc9GXfrH7MOKNQ_nu6pc6Gish2v_iuUEcpPHXrfqzGb9IHCLvfuLSaTDcYKYjQaEUAp-1uDW8-5posjiUV2eBiU48ajYg\", \"token_type\":\"Bearer\", \"expires_in\":\"299\"}"));

        //3  getOrganizationById

        Organization organization = new Organization(UUID.randomUUID(), "Free Press Organization", UUID.randomUUID());
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(organization)));

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setResponseCode(200).setBody("{\"access_token\": \"eyJraWQiOiJlOGQ3MjIzMC1iMDgwLTRhZjEtODFkOC0zMzE3NmNhMTM5ODIiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJhdWQiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJuYmYiOjE3MTQ3NTY2ODIsImlzcyI6Imh0dHA6Ly9teS1zZXJ2ZXI6OTAwMSIsImV4cCI6MTcxNDc1Njk4MiwiaWF0IjoxNzE0NzU2NjgyLCJqdGkiOiI0NDBlZDY0My00MzdkLTRjOTMtYTZkMi1jNzYxNjFlNDRlZjUifQ.fjqgoczZbbmcnvYpVN4yakpbplp7EkDyxslvar5nXBFa6mgIFcZa29fwIKfcie3oUMQ8MDWxayak5PZ_QIuHwTvKSWHs0WL91ljf-GT1sPi1b4gDKf0rJOwi0ClcoTCRIx9-WGR6t2BBR1Rk6RGF2MW7xKw8M-RMac2A2mPEPJqoh4Pky1KgxhZpEXixegpAdQIvBgc0KBZeQme-ZzTYugB8EPUmGpMlfd-zX_vcR1ijxi8e-LRRJMqmGkc9GXfrH7MOKNQ_nu6pc6Gish2v_iuUEcpPHXrfqzGb9IHCLvfuLSaTDcYKYjQaEUAp-1uDW8-5posjiUV2eBiU48ajYg\", \"token_type\":\"Bearer\", \"expires_in\":\"299\"}"));

        // 4 getRolesByOrganizationId
        json = "[Role{id=a617b9c7-c46a-41cf-97c3-cbeee3c454e7, name='AppleTreeCareTaker', userId=1f442dab-96a3-459e-8605-7f5cd5f82e25, roleOrganization=null}]";
        List<Role> roles = List.of(new Role(UUID.fromString("a617b9c7-c46a-41cf-97c3-cbeee3c454e7"), "AppleTreeCareTaker", UUID.fromString("1f442dab-96a3-459e-8605-7f5cd5f82e25"), null));
        RestPage<Role> restPage = new RestPage<>(roles, 1, 1, 1, 1, 1);

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(restPage)));

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setResponseCode(200).setBody("{\"access_token\": \"eyJraWQiOiJlOGQ3MjIzMC1iMDgwLTRhZjEtODFkOC0zMzE3NmNhMTM5ODIiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJhdWQiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJuYmYiOjE3MTQ3NTY2ODIsImlzcyI6Imh0dHA6Ly9teS1zZXJ2ZXI6OTAwMSIsImV4cCI6MTcxNDc1Njk4MiwiaWF0IjoxNzE0NzU2NjgyLCJqdGkiOiI0NDBlZDY0My00MzdkLTRjOTMtYTZkMi1jNzYxNjFlNDRlZjUifQ.fjqgoczZbbmcnvYpVN4yakpbplp7EkDyxslvar5nXBFa6mgIFcZa29fwIKfcie3oUMQ8MDWxayak5PZ_QIuHwTvKSWHs0WL91ljf-GT1sPi1b4gDKf0rJOwi0ClcoTCRIx9-WGR6t2BBR1Rk6RGF2MW7xKw8M-RMac2A2mPEPJqoh4Pky1KgxhZpEXixegpAdQIvBgc0KBZeQme-ZzTYugB8EPUmGpMlfd-zX_vcR1ijxi8e-LRRJMqmGkc9GXfrH7MOKNQ_nu6pc6Gish2v_iuUEcpPHXrfqzGb9IHCLvfuLSaTDcYKYjQaEUAp-1uDW8-5posjiUV2eBiU48ajYg\", \"token_type\":\"Bearer\", \"expires_in\":\"299\"}"));

        // 5 getUsersInOrganizationId
        //List<User> userList = List.of(new User(UUID.fromString("5eb2eb31-e80c-4924-be00-50a96b12aa3b"), "test6@sonam.email"), new User(UUID.fromString("1f442dab-96a3-459e-8605-7f5cd5f82e25"), "tom@tom.com"));
        List<UUID> userIds = List.of(UUID.fromString("1f442dab-96a3-459e-8605-7f5cd5f82e25"), UUID.fromString("5eb2eb31-e80c-4924-be00-50a96b12aa3b"));
        RestPage<UUID> usersInOrg = new RestPage<>(userIds, 1,1,1,1,1);

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(usersInOrg)));

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setResponseCode(200).setBody("{\"access_token\": \"eyJraWQiOiJlOGQ3MjIzMC1iMDgwLTRhZjEtODFkOC0zMzE3NmNhMTM5ODIiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJhdWQiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJuYmYiOjE3MTQ3NTY2ODIsImlzcyI6Imh0dHA6Ly9teS1zZXJ2ZXI6OTAwMSIsImV4cCI6MTcxNDc1Njk4MiwiaWF0IjoxNzE0NzU2NjgyLCJqdGkiOiI0NDBlZDY0My00MzdkLTRjOTMtYTZkMi1jNzYxNjFlNDRlZjUifQ.fjqgoczZbbmcnvYpVN4yakpbplp7EkDyxslvar5nXBFa6mgIFcZa29fwIKfcie3oUMQ8MDWxayak5PZ_QIuHwTvKSWHs0WL91ljf-GT1sPi1b4gDKf0rJOwi0ClcoTCRIx9-WGR6t2BBR1Rk6RGF2MW7xKw8M-RMac2A2mPEPJqoh4Pky1KgxhZpEXixegpAdQIvBgc0KBZeQme-ZzTYugB8EPUmGpMlfd-zX_vcR1ijxi8e-LRRJMqmGkc9GXfrH7MOKNQ_nu6pc6Gish2v_iuUEcpPHXrfqzGb9IHCLvfuLSaTDcYKYjQaEUAp-1uDW8-5posjiUV2eBiU48ajYg\", \"token_type\":\"Bearer\", \"expires_in\":\"299\"}"));


        List<User> userList = List.of(new me.sonam.authzmanager.clients.user.User(UUID.fromString("1f442dab-96a3-459e-8605-7f5cd5f82e25")),
                new me.sonam.authzmanager.clients.user.User(UUID.fromString("5eb2eb31-e80c-4924-be00-50a96b12aa3b")));
        //6 getUserByBatchOfIds
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(userList)));

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setResponseCode(200).setBody("{\"access_token\": \"eyJraWQiOiJlOGQ3MjIzMC1iMDgwLTRhZjEtODFkOC0zMzE3NmNhMTM5ODIiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJhdWQiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJuYmYiOjE3MTQ3NTY2ODIsImlzcyI6Imh0dHA6Ly9teS1zZXJ2ZXI6OTAwMSIsImV4cCI6MTcxNDc1Njk4MiwiaWF0IjoxNzE0NzU2NjgyLCJqdGkiOiI0NDBlZDY0My00MzdkLTRjOTMtYTZkMi1jNzYxNjFlNDRlZjUifQ.fjqgoczZbbmcnvYpVN4yakpbplp7EkDyxslvar5nXBFa6mgIFcZa29fwIKfcie3oUMQ8MDWxayak5PZ_QIuHwTvKSWHs0WL91ljf-GT1sPi1b4gDKf0rJOwi0ClcoTCRIx9-WGR6t2BBR1Rk6RGF2MW7xKw8M-RMac2A2mPEPJqoh4Pky1KgxhZpEXixegpAdQIvBgc0KBZeQme-ZzTYugB8EPUmGpMlfd-zX_vcR1ijxi8e-LRRJMqmGkc9GXfrH7MOKNQ_nu6pc6Gish2v_iuUEcpPHXrfqzGb9IHCLvfuLSaTDcYKYjQaEUAp-1uDW8-5posjiUV2eBiU48ajYg\", \"token_type\":\"Bearer\", \"expires_in\":\"299\"}"));

         Role role = new Role(UUID.fromString("a617b9c7-c46a-41cf-97c3-cbeee3c454e7"), "AppleCareTaker", UUID.fromString("1f442dab-96a3-459e-8605-7f5cd5f82e25"), null);
        me.sonam.authzmanager.controller.admin.clients.carrier.User user = new me.sonam.authzmanager.controller.admin.clients.carrier.User(UUID.fromString("1f442dab-96a3-459e-8605-7f5cd5f82e25"), role);
        List<ClientOrganizationUserWithRole> clientOrganizations = List.of(
                new ClientOrganizationUserWithRole(UUID.fromString("f8b36547-9f1e-4905-b726-e50e76a9076b"), UUID.fromString("18a528d0-8686-4ecc-ae7e-fba9a8654f5b"), user));

        //getClientOrganizationUserWithRoles
        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(clientOrganizations)));
        //"{\"clientId\":\"f8b36547-9f1e-4905-b726-e50e76a9076b\", \"organizationId\":\"18a528d0-8686-4ecc-ae7e-fba9a8654f5b\", " +
        //        "\"user\":\"User\"{\"id\":\"1f442dab-96a3-459e-8605-7f5cd5f82e25\", \"firstName\":\"null\", \"lastName\":\"null\", \"email='null', authenticationId='null', role=Role{id=a617b9c7-c46a-41cf-97c3-cbeee3c454e7, name='AppleTreeCareTaker', userId=1f442dab-96a3-459e-8605-7f5cd5f82e25, roleOrganization=null}}}, ClientOrganziationUserWithRole{clientId=f8b36547-9f1e-4905-b726-e50e76a9076b, organizationId=18a528d0-8686-4ecc-ae7e-fba9a8654f5b, user=User{id=5eb2eb31-e80c-4924-be00-50a96b12aa3b, firstName='null', lastName='null', email='null', authenticationId='null', role=Role{id=a617b9c7-c46a-41cf-97c3-cbeee3c454e7, name='AppleTreeCareTaker', userId=1f442dab-96a3-459e-8605-7f5cd5f82e25, roleOrganization=null}}}]


        EntityExchangeResult<String> entityExchangeResult = webTestClient.get()
                .uri("/admin/clients/"+oauthClient.getId()+"/users")
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();
        LOG.info("response: {}", entityExchangeResult.getResponseBody());

        // take request for mocked response of access token
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/oauth2/token");

        //1
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/issuer/clients");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/oauth2/token");

        //2
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/issuer/clients/"+oauthClient.getId()+"/organizations/id");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/oauth2/token");

        //3
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/oauth2/token");

        //4
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/organizations/");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/oauth2/token");

        //5
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/organizations/");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/oauth2/token");

        //6
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/users/ids/");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/oauth2/token");

        //7
        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/roles/client-organization-users/client-id/"
                +oauthClient.getId()+"/organization-id");
    }

    private String saveOauthClient() throws InterruptedException {
       OauthClient oauthClient = getOauthClient();
        RegisteredClient registeredClient = oauthClient.getRegisteredClient();
        Map<String, Object> map = registeredClientUtil.getMapObject(registeredClient);

        //BodyInserters.FormInserter<String> formInserter = getFormInserter(oauthClient);
        //String json = getJson(map);
        //LOG.info("setting json as response for client rest service: {}", json);

        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setResponseCode(200).setBody("{\"access_token\": \"eyJraWQiOiJlOGQ3MjIzMC1iMDgwLTRhZjEtODFkOC0zMzE3NmNhMTM5ODIiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJhdWQiOiI3NzI1ZjZmZC1kMzk2LTQwYWYtOTg4Ni1jYTg4YzZlOGZjZDgiLCJuYmYiOjE3MTQ3NTY2ODIsImlzcyI6Imh0dHA6Ly9teS1zZXJ2ZXI6OTAwMSIsImV4cCI6MTcxNDc1Njk4MiwiaWF0IjoxNzE0NzU2NjgyLCJqdGkiOiI0NDBlZDY0My00MzdkLTRjOTMtYTZkMi1jNzYxNjFlNDRlZjUifQ.fjqgoczZbbmcnvYpVN4yakpbplp7EkDyxslvar5nXBFa6mgIFcZa29fwIKfcie3oUMQ8MDWxayak5PZ_QIuHwTvKSWHs0WL91ljf-GT1sPi1b4gDKf0rJOwi0ClcoTCRIx9-WGR6t2BBR1Rk6RGF2MW7xKw8M-RMac2A2mPEPJqoh4Pky1KgxhZpEXixegpAdQIvBgc0KBZeQme-ZzTYugB8EPUmGpMlfd-zX_vcR1ijxi8e-LRRJMqmGkc9GXfrH7MOKNQ_nu6pc6Gish2v_iuUEcpPHXrfqzGb9IHCLvfuLSaTDcYKYjQaEUAp-1uDW8-5posjiUV2eBiU48ajYg\", \"token_type\":\"Bearer\", \"expires_in\":\"299\"}"));


        mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setResponseCode(200).setBody(getJson(map)));


        EntityExchangeResult<String> entityExchangeResult = webTestClient.post().uri("/admin/clients")
              //  .bodyValue(getFormInserter(oauthClient))
                .body(getFormInserter(oauthClient))
                .headers(httpHeaders -> httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED))
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();
        LOG.info("response: {}", entityExchangeResult.getResponseBody());

        // take request for mocked response of access token
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/oauth2/token");

        recordedRequest = mockWebServer.takeRequest();
        Assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        Assertions.assertThat(recordedRequest.getPath()).startsWith("/issuer/clients");

        return oauthClient.getFullClientId();
    }

    private BodyInserters.FormInserter<String> getFormInserter(OauthClient oauthClient) {
        BodyInserters.FormInserter<String> formInserter =
                BodyInserters.fromFormData("id", oauthClient.getId()==null ? "": oauthClient.getId())
                        .with("clientId", oauthClient.getClientId())
                        .with("clientSecret", oauthClient.getClientSecret())

                        .with("scopes", oauthClient.getScopes().toString())
                        .with("customScopes", oauthClient.getCustomScopes())
                        .with("redirectUris", oauthClient.getRedirectUris())
                        .with("postLogoutRedirecUris", oauthClient.getPostLogoutRedirectUris());

        if (!oauthClient.getClientAuthenticationMethods().isEmpty()) {
            oauthClient.getClientAuthenticationMethods().forEach(s -> formInserter.with("clientAuthenticationMethods", s));
        }
        if (!oauthClient.getAuthorizationGrantTypes().isEmpty()) {
            oauthClient.getAuthorizationGrantTypes().forEach(s -> formInserter.with("authorizationGrantTypes", s));
        }

        if (oauthClient.getClientSettings() != null) {
            formInserter.with("clientSettings.requireAuthorizationConsent", String.valueOf(oauthClient.getClientSettings().isRequireAuthorizationConsent()));
            formInserter.with("clientSettings.requireProofKey", String.valueOf(oauthClient.getClientSettings().isRequireProofKey()));
            formInserter.with("clientSettings.jwkSetUrl", oauthClient.getClientSettings().getJwkSetUrl());
        }

        return formInserter;
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
