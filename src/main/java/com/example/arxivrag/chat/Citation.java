package com.example.arxivrag.chat;

import java.util.List;

/**
 * DTO representing an academic paper citation resolved from canonical PostgreSQL metadata.
 */
public record Citation(
    String arxivId,
    String title,
    List<String> authors,
    String url
) {
    public Citation(String arxivId, String title, List<String> authors) {
        this(arxivId, title, authors, "https://arxiv.org/abs/" + arxivId);
    }
}
