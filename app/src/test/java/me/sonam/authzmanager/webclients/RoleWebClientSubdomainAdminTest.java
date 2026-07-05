package me.sonam.authzmanager.webclients;

import me.sonam.authzmanager.clients.role.AuthzManagerRoleAssignment;
import me.sonam.authzmanager.rest.RestPage;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RoleWebClientSubdomainAdminTest {
    private MockWebServer server;
    private RoleWebClient roleWebClient;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        roleWebClient = new RoleWebClient(WebClient.builder(), server.url("/roles").toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void listsAddsAndRemovesSubdomainAdminAssignments() throws InterruptedException {
        UUID subdomainId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        String assignmentJson = """
                {"id":"%s","authzManagerRoleId":"%s","userId":"%s","scopeType":"SUBDOMAIN","scopeId":"%s"}
                """.formatted(assignmentId, roleId, userId, subdomainId);

        server.enqueue(jsonResponse("{" +
                "\"content\":[" + assignmentJson + "]," +
                "\"number\":0,\"size\":10,\"totalElements\":1}"));
        RestPage<AuthzManagerRoleAssignment> page = roleWebClient
                .getSubdomainAdminAssignments("token", subdomainId, PageRequest.of(0, 10)).block();
        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().id()).isEqualTo(assignmentId);
        RecordedRequest listRequest = server.takeRequest();
        assertThat(listRequest.getPath()).isEqualTo(
                "/roles/authzmanagerroles/subdomains/" + subdomainId + "/administrators?page=0&size=10");
        assertThat(listRequest.getHeader("Authorization")).isEqualTo("Bearer token");

        server.enqueue(jsonResponse(assignmentJson));
        AuthzManagerRoleAssignment added = roleWebClient
                .addSubdomainAdmin("token", subdomainId, userId).block();
        assertThat(added.id()).isEqualTo(assignmentId);
        RecordedRequest addRequest = server.takeRequest();
        assertThat(addRequest.getMethod()).isEqualTo("POST");
        assertThat(addRequest.getPath()).isEqualTo(
                "/roles/authzmanagerroles/subdomains/" + subdomainId + "/administrators/" + userId);

        server.enqueue(jsonResponse("{\"message\":\"SubdomainAdmin assignment deleted\"}"));
        String removed = roleWebClient.removeSubdomainAdmin("token", subdomainId, assignmentId).block();
        assertThat(removed).contains("SubdomainAdmin assignment deleted");
        RecordedRequest removeRequest = server.takeRequest();
        assertThat(removeRequest.getMethod()).isEqualTo("DELETE");
        assertThat(removeRequest.getPath()).isEqualTo(
                "/roles/authzmanagerroles/subdomains/" + subdomainId + "/administrators/" + assignmentId);
    }

    private MockResponse jsonResponse(String body) {
        return new MockResponse().setHeader("Content-Type", "application/json").setResponseCode(200).setBody(body);
    }
}
