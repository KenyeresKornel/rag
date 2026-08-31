package com.example.arxivrag.vector;

/**
 * Result of a similarity search from the vector database.
 */
public record VectorSearchResult(
    String paperId,
    String chunkId,
    double score
) {}
