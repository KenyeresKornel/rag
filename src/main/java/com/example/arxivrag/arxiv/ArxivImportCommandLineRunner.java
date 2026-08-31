package com.example.arxivrag.arxiv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class ArxivImportCommandLineRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ArxivImportCommandLineRunner.class);

    private final ArxivImportService importService;
    private final ApplicationContext applicationContext;

    public ArxivImportCommandLineRunner(ArxivImportService importService, ApplicationContext applicationContext) {
        this.importService = importService;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length > 0 && "import-arxiv".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                log.error("Missing dataset file path. Usage: import-arxiv <path-to-json-file>");
                System.exit(1);
            }

            String filePath = args[1];
            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                log.error("Dataset file does not exist or is not a file: {}", filePath);
                System.exit(1);
            }

            try {
                importService.importDataset(file);
                log.info("Import completed successfully. Exiting.");
                SpringApplication.exit(applicationContext, () -> 0);
                System.exit(0);
            } catch (Exception e) {
                log.error("Fatal error during arXiv dataset import", e);
                SpringApplication.exit(applicationContext, () -> 1);
                System.exit(1);
            }
        }
    }
}
