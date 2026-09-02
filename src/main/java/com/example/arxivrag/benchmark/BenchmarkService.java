package com.example.arxivrag.benchmark;

import com.example.arxivrag.vector.VectorSearchRequest;
import com.example.arxivrag.vector.VectorStoreGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service executing high-precision vector database query performance and latency benchmarks.
 */
@Service
public class BenchmarkService {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkService.class);

    private static final String[] BENCHMARK_QUERIES = {
        "neural networks", "transformer model", "large language models", "reinforcement learning",
        "deep learning architectures", "natural language processing", "computer vision",
        "semantic search retrieval", "attention mechanisms", "grounded prompt generation",
        "vector database indexing", "cosine similarity search", "supervised classification",
        "unsupervised clustering", "generative adversarial networks"
    };

    private final VectorStoreGateway vectorStoreGateway;
    private final Environment environment;

    @Autowired
    public BenchmarkService(VectorStoreGateway vectorStoreGateway, Environment environment) {
        this.vectorStoreGateway = vectorStoreGateway;
        this.environment = environment;
    }

    /**
     * Executes a complete performance benchmark, including JIT warm-up, timing loop, and statistical compilation.
     */
    public BenchmarkResult runBenchmark(int totalQueries, int topK) {
        String activeStore = getActiveStoreProfile();
        log.info("Starting performance benchmark on active store [{}] executing {} queries...", activeStore, totalQueries);

        // 1. JVM JIT Warm-up phase (to ensure hot path optimizations are loaded before timing begins)
        log.info("Initializing JVM JIT compiler warm-up phase (10 queries)...");
        for (int i = 0; i < 10; i++) {
            String query = BENCHMARK_QUERIES[i % BENCHMARK_QUERIES.length];
            vectorStoreGateway.search(new VectorSearchRequest(query, topK));
        }
        log.info("Warm-up completed successfully. Starting timed execution loop...");

        // 2. Timed Query Loop
        List<Double> latenciesMs = new ArrayList<>();
        long totalStartTimeNano = System.nanoTime();

        for (int i = 0; i < totalQueries; i++) {
            String query = BENCHMARK_QUERIES[i % BENCHMARK_QUERIES.length];

            long startQueryNano = System.nanoTime();
            vectorStoreGateway.search(new VectorSearchRequest(query, topK));
            long durationQueryNano = System.nanoTime() - startQueryNano;

            latenciesMs.add(durationQueryNano / 1_000_000.0); // Convert nanoseconds to milliseconds
        }

        long totalDurationNano = System.nanoTime() - totalStartTimeNano;

        // 3. Compile Metrics and Percentiles
        Collections.sort(latenciesMs); // Sort ascending for percentile indices

        double min = latenciesMs.get(0);
        double max = latenciesMs.get(latenciesMs.size() - 1);
        double sum = latenciesMs.stream().mapToDouble(Double::doubleValue).sum();
        double avg = sum / latenciesMs.size();

        double p50 = calculatePercentile(latenciesMs, 0.50);
        double p95 = calculatePercentile(latenciesMs, 0.95);

        double totalDurationSec = totalDurationNano / 1_000_000_000.0;
        double qps = totalQueries / totalDurationSec;

        BenchmarkResult result = new BenchmarkResult(activeStore, totalQueries, topK, min, max, avg, p50, p95, qps);
        log.info("Benchmark complete: Average Latency: {} ms | Throughput: {} QPS", String.format("%.2f", avg), String.format("%.2f", qps));
        return result;
    }

    /**
     * Helper to retrieve active database profile.
     */
    public String getActiveStoreProfile() {
        if (environment == null) {
            return "store-pgvector (default)";
        }
        for (String profile : environment.getActiveProfiles()) {
            if (profile.startsWith("store-")) {
                return profile;
            }
        }
        return "store-pgvector"; // default fallback
    }

    /**
     * Programmatically extracts exact percentile values from a sorted list of durations.
     */
    public double calculatePercentile(List<Double> sortedList, double percentile) {
        if (sortedList == null || sortedList.isEmpty()) {
            return 0.0;
        }
        int index = (int) Math.ceil(percentile * sortedList.size()) - 1;
        index = Math.max(0, Math.min(index, sortedList.size() - 1));
        return sortedList.get(index);
    }
}
