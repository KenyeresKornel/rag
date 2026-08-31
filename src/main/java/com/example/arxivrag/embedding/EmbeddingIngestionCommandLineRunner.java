package com.example.arxivrag.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * CommandLineRunner executing the embedding ingestion pipeline when called with 'embed-papers'.
 */
@Component
public class EmbeddingIngestionCommandLineRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingIngestionCommandLineRunner.class);

    private final EmbeddingIngestionService ingestionService;
    private final ApplicationContext applicationContext;

    @Autowired
    public EmbeddingIngestionCommandLineRunner(
            EmbeddingIngestionService ingestionService,
            ApplicationContext applicationContext) {
        this.ingestionService = ingestionService;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length > 0 && "embed-papers".equals(args[0])) {
            log.info("CLI Argument 'embed-papers' detected. Initializing embedding ingestion pipeline...");
            try {
                EmbeddingIngestionService.IngestionResult result = ingestionService.ingest();
                log.info("Paper embedding ingestion finished successfully! Summary: {}", result);
                
                int exitCode = SpringApplication.exit(applicationContext, () -> 0);
                System.exit(exitCode);
            } catch (Exception e) {
                log.error("Fatal error during CLI paper embedding ingestion", e);
                int exitCode = SpringApplication.exit(applicationContext, () -> 1);
                System.exit(exitCode);
            }
        }
    }
}
