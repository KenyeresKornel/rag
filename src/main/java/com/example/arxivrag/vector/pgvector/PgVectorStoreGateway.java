package com.example.arxivrag.vector.pgvector;

import com.example.arxivrag.embedding.RetrievalDocument;
import com.example.arxivrag.vector.VectorSearchRequest;
import com.example.arxivrag.vector.VectorSearchResult;
import com.example.arxivrag.vector.VectorStoreGateway;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link VectorStoreGateway} for PostgreSQL pgvector using Spring AI's {@link VectorStore}.
 */
@Component
@Profile("store-pgvector")
public class PgVectorStoreGateway implements VectorStoreGateway {

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public PgVectorStoreGateway(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void save(List<RetrievalDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        List<Document> springAiDocs = documents.stream()
            .map(doc -> {
                // Generate a deterministic UUID from the documentId so Spring AI's PgVectorStore can parse it safely
                String uuidStr = UUID.nameUUIDFromBytes(doc.documentId().getBytes(StandardCharsets.UTF_8)).toString();
                return new Document(uuidStr, doc.text(), doc.metadata());
            })
            .toList();
        vectorStore.add(springAiDocs);
    }

    @Override
    public void save(List<RetrievalDocument> documents, List<float[]> embeddings) {
        if (documents == null || documents.isEmpty() || embeddings == null || embeddings.isEmpty()) {
            return;
        }
        
        String sql = "INSERT INTO vector_store (id, content, metadata, embedding) VALUES (?, ?, ?::jsonb, ?::vector)";
        
        for (int i = 0; i < documents.size(); i++) {
            RetrievalDocument doc = documents.get(i);
            float[] embedding = embeddings.get(i);
            
            // Replicate Spring AI's name-based UUID generation to ensure perfect retrieval/deletion mapping
            UUID docUuid = UUID.nameUUIDFromBytes(doc.documentId().getBytes(StandardCharsets.UTF_8));
            
            String metadataJson;
            try {
                metadataJson = objectMapper.writeValueAsString(doc.metadata());
            } catch (Exception e) {
                metadataJson = "{}";
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int j = 0; j < embedding.length; j++) {
                sb.append(embedding[j]);
                if (j < embedding.length - 1) {
                    sb.append(",");
                }
            }
            sb.append("]");
            String vectorStr = sb.toString();
            
            jdbcTemplate.update(sql, docUuid, doc.text(), metadataJson, vectorStr);
        }
    }

    @Override
    public List<VectorSearchResult> search(VectorSearchRequest request) {
        if (request == null) {
            return Collections.emptyList();
        }

        // Optimization: If the vector store is empty, avoid calling the external EmbeddingModel or running similarity math
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM vector_store", Integer.class);
        if (count == null || count == 0) {
            return Collections.emptyList();
        }

        if (request.queryEmbedding() != null) {
            // Perform raw SQL similarity search when pre-computed embedding is provided (highly useful for custom testing and score verification)
            float[] queryEmbedding = request.queryEmbedding();
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < queryEmbedding.length; i++) {
                sb.append(queryEmbedding[i]);
                if (i < queryEmbedding.length - 1) {
                    sb.append(",");
                }
            }
            sb.append("]");
            String vectorStr = sb.toString();

            String sql = "SELECT metadata->>'paper_id' as paper_id, metadata->>'chunk_id' as chunk_id, 1 - (embedding <=> ?::vector) as score " +
                         "FROM vector_store " +
                         "ORDER BY embedding <=> ?::vector " +
                         "LIMIT ?";
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                String paperId = rs.getString("paper_id");
                String chunkId = rs.getString("chunk_id");
                double score = rs.getDouble("score");
                return new VectorSearchResult(paperId, chunkId, score);
            }, vectorStr, vectorStr, request.topK());
        } else {
            // Standard semantic search using Spring AI VectorStore
            SearchRequest.Builder builder = SearchRequest.builder()
                .query(request.query())
                .topK(request.topK())
                .similarityThreshold(0.0); // Ensure low-scoring conceptual matches are not silently filtered out

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
    }

    @Override
    public void deleteByPaperIds(List<String> paperIds) {
        if (paperIds == null || paperIds.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", Collections.nCopies(paperIds.size(), "?"));
        String sql = "DELETE FROM vector_store WHERE metadata->>'paper_id' IN (" + placeholders + ")";
        jdbcTemplate.update(sql, paperIds.toArray());
    }
}
