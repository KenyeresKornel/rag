package com.example.arxivrag.benchmark;

import com.example.arxivrag.embedding.RetrievalDocument;
import com.example.arxivrag.paper.Paper;
import com.example.arxivrag.paper.PaperRepository;
import com.example.arxivrag.vector.VectorStoreGateway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class BenchmarkControllerIntegrationTests {

    @Autowired
    private BenchmarkController benchmarkController;

    @Autowired
    private PaperRepository paperRepository;

    @Autowired
    private VectorStoreGateway vectorStoreGateway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM vector_store");
        jdbcTemplate.update("DELETE FROM embedding_checkpoints");
        jdbcTemplate.update("DELETE FROM papers");
    }

    @Test
    void testBenchmarkControllerEndpointTelemetry() {
        // Arrange: Seed at least 1 paper and vector so that search runs cleanly without empty return earlys
        Paper paper = new Paper(
            "arxiv-bench", "Benchmarking Paper", "Deep latency measurements.",
            List.of("Szepesi"), List.of("cs.CL"), LocalDate.now(), null, null
        );
        paperRepository.save(paper);

        vectorStoreGateway.save(List.of(new RetrievalDocument(
            "arxiv:arxiv-bench:chunk:0", "arxiv-bench", "Benchmarking Paper\nDeep latency.",
            Map.of("paper_id", "arxiv-bench", "chunk_id", "0"), "h-bench"
        )));

        // Act: Directly execute controller endpoint
        BenchmarkResult res = benchmarkController.runBenchmark();

        // Assert: Verify all statistical keys are present and mapped as non-null numeric/string values
        assertThat(res).isNotNull();
        assertThat(res.activeStoreProfile()).isEqualTo("store-pgvector");
        assertThat(res.totalQueries()).isEqualTo(50);
        assertThat(res.topK()).isEqualTo(5);
        
        assertThat(res.minLatencyMs()).isGreaterThanOrEqualTo(0.0);
        assertThat(res.maxLatencyMs()).isGreaterThanOrEqualTo(0.0);
        assertThat(res.avgLatencyMs()).isGreaterThanOrEqualTo(0.0);
        assertThat(res.p50LatencyMs()).isGreaterThanOrEqualTo(0.0);
        assertThat(res.p95LatencyMs()).isGreaterThanOrEqualTo(0.0);
        assertThat(res.qps()).isGreaterThanOrEqualTo(0.0);
    }
}
