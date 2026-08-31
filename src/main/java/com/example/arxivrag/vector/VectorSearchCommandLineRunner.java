package com.example.arxivrag.vector;

import com.example.arxivrag.paper.Paper;
import com.example.arxivrag.paper.PaperRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * CommandLineRunner executing semantic similarity searches against pgvector when called with 'search-vector'.
 */
@Component
public class VectorSearchCommandLineRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(VectorSearchCommandLineRunner.class);

    private final VectorStoreGateway vectorStoreGateway;
    private final PaperRepository paperRepository;
    private final ApplicationContext applicationContext;

    @Autowired
    public VectorSearchCommandLineRunner(
            VectorStoreGateway vectorStoreGateway,
            PaperRepository paperRepository,
            ApplicationContext applicationContext) {
        this.vectorStoreGateway = vectorStoreGateway;
        this.paperRepository = paperRepository;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length > 0 && "search-vector".equals(args[0])) {
            if (args.length < 2) {
                System.out.println("Error: Missing query string. Usage: search-vector \"<query>\"");
                int exitCode = SpringApplication.exit(applicationContext, () -> 1);
                System.exit(exitCode);
            }

            // Group all subsequent arguments into a single search query (handles both quoted and unquoted inputs)
            String searchQuery = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
            System.out.println("\n>>> Performing semantic similarity search for: \"" + searchQuery + "\"\n");

            try {
                VectorSearchRequest searchRequest = new VectorSearchRequest(searchQuery, 5); // Fetch top-5 results
                List<VectorSearchResult> results = vectorStoreGateway.search(searchRequest);

                if (results.isEmpty()) {
                    System.out.println("0 semantically relevant papers found.\n");
                    int exitCode = SpringApplication.exit(applicationContext, () -> 0);
                    System.exit(exitCode);
                }

                // Resolve metadata back from canonical PostgreSQL papers table
                List<String> paperIds = results.stream().map(VectorSearchResult::paperId).toList();
                List<Paper> papers = paperRepository.findAllByArxivIdIn(paperIds);

                Map<String, Paper> paperMap = papers.stream()
                    .collect(Collectors.toMap(Paper::getArxivId, Function.identity()));

                // Print beautiful tabular representation
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.printf("%-10s | %-12s | %s%n", "Score", "arXiv ID", "Paper Title / Abstract Snippet");
                System.out.println("----------------------------------------------------------------------------------------------------");

                for (VectorSearchResult res : results) {
                    Paper p = paperMap.get(res.paperId());
                    if (p != null) {
                        System.out.printf("%.4f     | %-12s | %s%n", res.score(), res.paperId(), p.getTitle());
                        String snip = p.getAbstractText().length() > 140 
                            ? p.getAbstractText().replaceAll("\\s+", " ").substring(0, 137).trim() + "..." 
                            : p.getAbstractText().replaceAll("\\s+", " ").trim();
                        System.out.printf("%-10s | %-12s | Snippet: %s%n", "", "", snip);
                        System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - ");
                    }
                }
                System.out.println("----------------------------------------------------------------------------------------------------\n");

                int exitCode = SpringApplication.exit(applicationContext, () -> 0);
                System.exit(exitCode);
            } catch (Exception e) {
                log.error("Fatal error during CLI semantic similarity search", e);
                int exitCode = SpringApplication.exit(applicationContext, () -> 1);
                System.exit(exitCode);
            }
        }
    }
}
