package com.example.arxivrag;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "openai.api-key=super-secret-token"
)
class ArxivRagApplicationTests {

    @LocalServerPort
    private int port;

    @Test
    void contextLoads() {
    }

    @Test
    void servesFrontendShell() throws Exception {
        var response = get("/");

        assertThat(response.statusCode()).isBetween(200, 299);
        assertThat(response.body()).contains("arXiv RAG Lab");
    }

    @Test
    void exposesOnlySafeRuntimeConfig() throws Exception {
        var response = get("/api/runtime-config");

        assertThat(response.statusCode()).isBetween(200, 299);
        assertThat(response.body()).contains("\"configuredProfile\":\"test\"");
        assertThat(response.body()).contains("\"activeProfiles\":[\"test\"]");
        assertThat(response.body()).contains("\"features\"");
        assertThat(response.body()).doesNotContain("super-secret-token");
        assertThat(response.body()).doesNotContain("api-key");
        assertThat(response.body()).doesNotContain("password");
        assertThat(response.body()).doesNotContain("token");
    }

    private HttpResponse<String> get(String path) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .GET()
                .build();

        return HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
    }
}
