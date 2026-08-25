package com.example.arxivrag.config;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/health")
public class BackendHealthController {

    private final Environment environment;

    public BackendHealthController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping
    public BackendHealthResponse health() {
        return new BackendHealthResponse(
                "UP",
                environment.getProperty("spring.application.name", "arxiv-rag"),
                Instant.now(),
                "Backend services are reachable."
        );
    }

    public record BackendHealthResponse(
            String status,
            String applicationName,
            Instant checkedAt,
            String message
    ) {
    }
}
