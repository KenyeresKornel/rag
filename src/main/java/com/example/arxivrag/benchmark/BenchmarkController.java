package com.example.arxivrag.benchmark;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing the dynamic query latency benchmarking telemetry.
 */
@RestController
@RequestMapping("/api/benchmark")
public class BenchmarkController {

    private final BenchmarkService benchmarkService;

    @Autowired
    public BenchmarkController(BenchmarkService benchmarkService) {
        this.benchmarkService = benchmarkService;
    }

    /**
     * Executes a fast, local performance benchmark of 50 queries against the active vector database
     * and returns the compiled statistical metrics.
     */
    @GetMapping
    public BenchmarkResult runBenchmark() {
        return benchmarkService.runBenchmark(50, 5);
    }
}
