package com.example.arxivrag.benchmark;

import com.example.arxivrag.vector.VectorStoreGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BenchmarkServiceTests {

    private BenchmarkService benchmarkService;

    @BeforeEach
    void setUp() {
        VectorStoreGateway vectorStoreGateway = mock(VectorStoreGateway.class);
        Environment environment = mock(Environment.class);
        benchmarkService = new BenchmarkService(vectorStoreGateway, environment);
    }

    @Test
    void testScenario6_1_PercentileCalculations() {
        // Arrange: Prepare 10 mock sorted latencies (10.0ms, 20.0ms, ..., 100.0ms)
        List<Double> latencies = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            latencies.add(i * 10.0);
        }

        // Act
        double p50 = benchmarkService.calculatePercentile(latencies, 0.50);
        double p95 = benchmarkService.calculatePercentile(latencies, 0.95);

        // Assert: Median (p50) of 10 items maps exactly to index 4 (50.0 ms)
        assertThat(p50).isEqualTo(50.0);

        // Assert: Tail (p95) of 10 items maps exactly to index 9 (100.0 ms)
        assertThat(p95).isEqualTo(100.0);
    }

    @Test
    void testScenario6_1_EdgeCasesPercentiles() {
        // Assert empty and null lists return 0.0 gracefully
        assertThat(benchmarkService.calculatePercentile(Collections.emptyList(), 0.50)).isEqualTo(0.0);
        assertThat(benchmarkService.calculatePercentile(null, 0.50)).isEqualTo(0.0);
    }
}
