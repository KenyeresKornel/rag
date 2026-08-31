package com.example.arxivrag.embedding;

import java.util.Map;

/**
 * Represents a document chunk to be embedded and ingested into pgvector.
 */
public record RetrievalDocument(
    String documentId,
    String paperId,
    String text,
    Map<String, Object> metadata,
    String contentHash
) {}
