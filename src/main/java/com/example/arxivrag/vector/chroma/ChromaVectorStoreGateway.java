package com.example.arxivrag.vector.chroma;

import com.example.arxivrag.embedding.RetrievalDocument;
import com.example.arxivrag.vector.VectorSearchRequest;
import com.example.arxivrag.vector.VectorSearchResult;
import com.example.arxivrag.vector.VectorStoreGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link VectorStoreGateway} for Chroma DB using Spring AI's {@link VectorStore}.
 */
@Component
@Profile("store-chroma")
public class ChromaVectorStoreGateway implements VectorStoreGateway {

    private static final Logger log = LoggerFactory.getLogger(ChromaVectorStoreGateway.class);

    private final VectorStore vectorStore;

    @Autowired
    public ChromaVectorStoreGateway(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void save(List<RetrievalDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        log.info("Saving {} documents to Chroma Vector Store...", documents.size());
        List<Document> springAiDocs = documents.stream()
            .map(doc -> {
                // Generate a deterministic UUID string so the vector store can process it safely
                String uuidStr = UUID.nameUUIDFromBytes(doc.documentId().getBytes(StandardCharsets.UTF_8)).toString();
                return new Document(uuidStr, doc.text(), doc.metadata());
            })
            .toList();
        vectorStore.add(springAiDocs);
    }

    @Override
    public void save(List<RetrievalDocument> documents, List<float[]> embeddings) {
        throw new UnsupportedOperationException("User-provided embeddings are not supported by the current Spring AI version on Chroma.");
    }

    @Override
    public List<VectorSearchResult> search(VectorSearchRequest request) {
        if (request == null) {
            return Collections.emptyList();
        }

        SearchRequest.Builder builder = SearchRequest.builder()
            .topK(request.topK())
            .similarityThreshold(0.0); // Allow low-scoring conceptual matches to pass through

        builder.query(request.query() != null ? request.query() : "");

        if (request.filterExpression() != null) {
            builder.filterExpression(request.filterExpression());
        }

        SearchRequest springAiRequest = builder.build();
        List<Document> docs = vectorStore.similaritySearch(springAiRequest);

        return docs.stream()
            .map(doc -> {
                String paperId = (String) doc.getMetadata().get("paper_id");
                String chunkId = (String) doc.getMetadata().get("chunk_id");
                double score = doc.getScore() != null ? doc.getScore() : 0.0;
                return new VectorSearchResult(paperId, chunkId, score);
            })
            .toList();
    }

    @Override
    public void deleteByPaperIds(List<String> paperIds) {
        if (paperIds == null || paperIds.isEmpty()) {
            return;
        }
        log.info("Deleting documents from Chroma associated with paper IDs: {}", paperIds);
        
        for (String paperId : paperIds) {
            // Find existing document ID by searching with metadata filters
            org.springframework.ai.vectorstore.filter.FilterExpressionBuilder b = new org.springframework.ai.vectorstore.filter.FilterExpressionBuilder();
            SearchRequest request = SearchRequest.builder()
                .query("") // empty semantic query since we select strictly by filters
                .topK(10)
                .filterExpression(b.eq("paper_id", paperId).build()) // compile Op into Filter.Expression
                .similarityThreshold(0.0)
                .build();

            List<Document> docs = vectorStore.similaritySearch(request);
            if (!docs.isEmpty()) {
                List<String> docIds = docs.stream().map(Document::getId).toList();
                vectorStore.delete(docIds);
            }
        }
    }
}
