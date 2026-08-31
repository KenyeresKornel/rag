package com.example.arxivrag.vector;

/**
 * Request details for performing similarity searches in the vector store.
 */
public record VectorSearchRequest(
    String query,
    float[] queryEmbedding,
    int topK
) {
    public VectorSearchRequest(String query, int topK) {
        this(query, null, topK);
    }

    public VectorSearchRequest(float[] queryEmbedding, int topK) {
        this(null, queryEmbedding, topK);
    }
}
