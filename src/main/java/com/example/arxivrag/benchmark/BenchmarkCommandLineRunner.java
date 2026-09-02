package com.example.arxivrag.benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * CommandLineRunner executing the performance benchmark harness when called with 'run-benchmarks'.
 */
@Component
public class BenchmarkCommandLineRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkCommandLineRunner.class);

    private final BenchmarkService benchmarkService;
    private final ApplicationContext applicationContext;

    @Autowired
    public BenchmarkCommandLineRunner(BenchmarkService benchmarkService, ApplicationContext applicationContext) {
        this.benchmarkService = benchmarkService;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length > 0 && "run-benchmarks".equals(args[0])) {
            log.info("CLI Argument 'run-benchmarks' detected. Launching performance test harness...");

            try {
                // Execute benchmark with 100 queries, retrieving top-5 documents per query
                BenchmarkResult res = benchmarkService.runBenchmark(100, 5);

                System.out.println("\n======================================================================");
                System.out.println("VECTOR STORE PERFORMANCE BAKE-OFF REPORT");
                System.out.println("======================================================================");
                System.out.printf("Active Backend Profile: [%s]%n", res.activeStoreProfile());
                System.out.printf("Total Queries Run     : %d%n", res.totalQueries());
                System.out.printf("Retrieval topK Limit  : %d%n", res.topK());
                System.out.println("----------------------------------------------------------------------");
                System.out.printf("Throughput            : %.2f QPS (Queries Per Second)%n", res.qps());
                System.out.printf("Average Latency       : %.2f ms%n", res.avgLatencyMs());
                System.out.printf("p50 (Median Latency)  : %.2f ms%n", res.p50LatencyMs());
                System.out.printf("p95 (Tail Latency)    : %.2f ms%n", res.p95LatencyMs());
                System.out.printf("Min Latency           : %.2f ms%n", res.minLatencyMs());
                System.out.printf("Max Latency           : %.2f ms%n", res.maxLatencyMs());
                System.out.println("======================================================================\n");

                int exitCode = SpringApplication.exit(applicationContext, () -> 0);
                System.exit(exitCode);
            } catch (Exception e) {
                log.error("Fatal error during CLI performance benchmarks execution", e);
                int exitCode = SpringApplication.exit(applicationContext, () -> 1);
                System.exit(exitCode);
            }
        }
    }
}
