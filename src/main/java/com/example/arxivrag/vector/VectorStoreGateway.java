package com.example.arxivrag.vector;

import com.example.arxivrag.embedding.RetrievalDocument;
import java.util.List;

/**
 * Agnostic gateway interface for interacting with the vector store database.
 */
public interface VectorStoreGateway {

    /**
     * Saves a list of retrieval documents to the vector store.
     * Generates and registers their embeddings automatically.
     */
    void save(List<RetrievalDocument> documents);

    /**
     * Saves a list of retrieval documents along with pre-computed vector embeddings.
     */
    void save(List<RetrievalDocument> documents, List<float[]> embeddings);

    /**
     * Performs a similarity search over the vector store.
     */
    List<VectorSearchResult> search(VectorSearchRequest request);

    /**
     * Deletes all vectors associated with the given paper IDs.
     */
    void deleteByPaperIds(List<String> paperIds);
}
