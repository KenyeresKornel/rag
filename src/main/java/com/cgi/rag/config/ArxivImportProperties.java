package com.cgi.rag.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "arxiv.import")
public record ArxivImportProperties(
        @NotEmpty List<String> categories,
        @Positive Integer maxRecords
) {
    public boolean isUnlimited() {
        return maxRecords == null;
    }
}
