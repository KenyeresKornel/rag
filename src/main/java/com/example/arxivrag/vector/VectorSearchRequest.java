package com.example.arxivrag.vector;

import org.springframework.ai.vectorstore.filter.Filter;

/**
 * Request details for performing similarity searches in the vector store, supporting optional metadata filters.
 */
public record VectorSearchRequest(
    String query,
    float[] queryEmbedding,
    int topK,
    Filter.Expression filterExpression
) {
    public VectorSearchRequest(String query, int topK) {
        this(query, null, topK, null);
    }

    public VectorSearchRequest(float[] queryEmbedding, int topK) {
        this(null, queryEmbedding, topK, null);
    }

    public VectorSearchRequest(String query, int topK, Filter.Expression filterExpression) {
        this(query, null, topK, filterExpression);
    }
}
