package com.example.arxivrag.vector.pgvector;

import com.example.arxivrag.embedding.RetrievalDocument;
import com.example.arxivrag.vector.VectorSearchRequest;
import com.example.arxivrag.vector.VectorSearchResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

@ActiveProfiles("test")
@SpringBootTest
class PgVectorStoreGatewayIntegrationTests {

    @Autowired
    private PgVectorStoreGateway pgVectorStoreGateway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM vector_store");
    }

    private float[] createVector(float v0, float v1) {
        float[] vector = new float[1536];
        vector[0] = v0;
        vector[1] = v1;
        // Remaining 1534 dimensions default to 0.0f
        return vector;
    }

    @Test
    void testScenario4_2_CosineSimilarityRetrievalCorrectness() {
        // Arrange: Prepare 3 documents with deterministic vector orientations
        RetrievalDocument docA = new RetrievalDocument(
            "arxiv:doc-a:chunk:0", "doc-a", "Document A Content",
            Map.of("paper_id", "doc-a", "chunk_id", "0"), "hash-a"
        );
        RetrievalDocument docB = new RetrievalDocument(
            "arxiv:doc-b:chunk:0", "doc-b", "Document B Content",
            Map.of("paper_id", "doc-b", "chunk_id", "0"), "hash-b"
        );
        RetrievalDocument docC = new RetrievalDocument(
            "arxiv:doc-c:chunk:0", "doc-c", "Document C Content",
            Map.of("paper_id", "doc-c", "chunk_id", "0"), "hash-c"
        );

        // Doc A vector: completely parallel to the query vector [1.0, 0.0]
        float[] vecA = createVector(1.0f, 0.0f);
        // Doc B vector: moderately similar [0.7071f, 0.7071f] (length = 1.0)
        float[] vecB = createVector(0.70710678f, 0.70710678f);
        // Doc C vector: orthogonal [0.0f, 1.0f]
        float[] vecC = createVector(0.0f, 1.0f);

        pgVectorStoreGateway.save(List.of(docA, docB, docC), List.of(vecA, vecB, vecC));

        // Diagnostics
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT id, content FROM vector_store");
        System.out.println(">>> DIAGNOSTICS: VECTOR_STORE SIZE = " + rows.size());
        for (Map<String, Object> r : rows) {
            System.out.println(">>> ROW ID: " + r.get("id") + " | CONTENT: " + r.get("content"));
        }

        // Act: Perform search using query embedding parallel to A: [1.0, 0.0]
        float[] queryEmbedding = createVector(1.0f, 0.0f);
        VectorSearchRequest searchRequest = new VectorSearchRequest(queryEmbedding, 2); // topK = 2

        List<VectorSearchResult> results = pgVectorStoreGateway.search(searchRequest);

        // Assert: Verify order (Doc A first, followed by Doc B. Doc C is excluded because topK = 2)
        assertThat(results).hasSize(2);

        VectorSearchResult firstResult = results.get(0);
        assertThat(firstResult.paperId()).isEqualTo("doc-a");
        // Cosine similarity for parallel unit vectors is exactly 1.0
        assertThat(firstResult.score()).isCloseTo(1.0, offset(1e-5));

        VectorSearchResult secondResult = results.get(1);
        assertThat(secondResult.paperId()).isEqualTo("doc-b");
        // Cosine similarity between [1.0, 0.0] and [0.7071, 0.7071] is exactly 0.7071
        assertThat(secondResult.score()).isCloseTo(0.70710678, offset(1e-5));
    }
}
