package com.example.arxivrag;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ArxivRagApplicationTests {

    @LocalServerPort
    private int port;

    @Test
    void contextLoads() {
    }

    @Test
    void servesFrontendShell() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/"))
                .GET()
                .build();
        var response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isBetween(200, 299);
        assertThat(response.body()).contains("arXiv RAG Lab");
    }
}
