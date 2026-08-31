package com.example.arxivrag.arxiv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ArxivRecord(
    String id,
    String submitter,
    String authors,
    String title,
    String comments,
    @JsonProperty("journal-ref") String journalRef,
    String doi,
    @JsonProperty("report-no") String reportNo,
    String categories,
    String license,
    @JsonProperty("abstract") String abstractText,
    List<Version> versions,
    @JsonProperty("update_date") String updateDate
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Version(
        String version,
        String created
    ) {}
}
