package com.example.arxivrag.config;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/runtime-config")
public class RuntimeConfigController {

    private final Environment environment;

    private final RagRuntimeProperties properties;

    public RuntimeConfigController(Environment environment, RagRuntimeProperties properties) {
        this.environment = environment;
        this.properties = properties;
    }

    @GetMapping
    public RuntimeConfigResponse runtimeConfig() {
        var activeProfiles = Arrays.stream(environment.getActiveProfiles()).toList();
        var displayedProfiles = activeProfiles.isEmpty()
                ? Arrays.stream(environment.getDefaultProfiles()).toList()
                : activeProfiles;
        var features = properties.getFeatures();

        return new RuntimeConfigResponse(
                environment.getProperty("spring.application.name", "arxiv-rag"),
                properties.getRuntime().getProfile(),
                displayedProfiles,
                List.of(
                        new FeatureFlag("Import", features.isImportEnabled()),
                        new FeatureFlag("Papers", features.isPapersEnabled()),
                        new FeatureFlag("Search", features.isSearchEnabled()),
                        new FeatureFlag("Chat", features.isChatEnabled()),
                        new FeatureFlag("Vector Stores", features.isVectorStoresEnabled()),
                        new FeatureFlag("Benchmarks", features.isBenchmarksEnabled())
                )
        );
    }

    public record RuntimeConfigResponse(
            String applicationName,
            String configuredProfile,
            List<String> activeProfiles,
            List<FeatureFlag> features
    ) {
    }

    public record FeatureFlag(String name, boolean enabled) {
    }
}
