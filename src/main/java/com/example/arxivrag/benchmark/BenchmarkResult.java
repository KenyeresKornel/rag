package com.example.arxivrag.benchmark;

/**
 * Record representing the compiled results of a vector database performance benchmark run.
 */
public record BenchmarkResult(
    String activeStoreProfile,
    int totalQueries,
    int topK,
    double minLatencyMs,
    double maxLatencyMs,
    double avgLatencyMs,
    double p50LatencyMs,
    double p95LatencyMs,
    double qps
) {}
