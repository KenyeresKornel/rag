package com.example.arxivrag.chat.tools;

import com.example.arxivrag.paper.Paper;
import com.example.arxivrag.paper.PaperRepository;
import com.example.arxivrag.vector.VectorSearchRequest;
import com.example.arxivrag.vector.VectorSearchResult;
import com.example.arxivrag.vector.VectorStoreGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Spring Configuration registering the callable LLM tools as Spring AI functions.
 */
@Configuration
public class SearchToolsConfig {

    private static final Logger log = LoggerFactory.getLogger(SearchToolsConfig.class);

    private final VectorStoreGateway vectorStoreGateway;
    private final PaperRepository paperRepository;

    @Autowired
    public SearchToolsConfig(VectorStoreGateway vectorStoreGateway, PaperRepository paperRepository) {
        this.vectorStoreGateway = vectorStoreGateway;
        this.paperRepository = paperRepository;
    }

    // Input DTOs for the functions
    public record SemanticSearchInput(String query, Integer topK) {}
    
    public record HybridSearchInput(String query, String category, Integer year, Integer topK) {}

    /**
     * Tool for performing pure semantic search over academic abstracts.
     */
    @Bean
    @Description("Searches arXiv academic papers semantically. Use this tool when the user asks general conceptual or research questions.")
    public Function<SemanticSearchInput, String> semanticSearch() {
        return input -> {
            String query = input.query();
            int topK = input.topK() != null ? input.topK() : 5;
            log.info("Agent Tool [semanticSearch] invoked for query: '{}' with topK: {}", query, topK);

            List<VectorSearchResult> results = vectorStoreGateway.search(new VectorSearchRequest(query, topK));
            if (results.isEmpty()) {
                return "No semantically relevant papers were found in the database.";
            }

            return fetchAndFormatPapers(results);
        };
    }

    /**
     * Tool for performing semantic search filtered by structured metadata constraints.
     */
    @Bean
    @Description("Searches arXiv papers semantically with structured metadata filters (category and year). Use this tool when the user restricts the search to a specific domain (e.g. computer vision 'cs.CV', computation and language 'cs.CL') or publication year.")
    public Function<HybridSearchInput, String> hybridSearch() {
        return input -> {
            String query = input.query();
            String category = input.category();
            Integer year = input.year();
            int topK = input.topK() != null ? input.topK() : 5;
            
            log.info("Agent Tool [hybridSearch] invoked for query: '{}' | category: {} | year: {} | topK: {}", 
                query, category, year, topK);

            // Build Spring AI Filter Expression programmatically using operators of type Op
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            
            FilterExpressionBuilder.Op categoryOp = null;
            if (category != null && !category.isEmpty()) {
                // categories is stored as a JSONB array, eq operator checks for presence of the tag
                categoryOp = b.eq("categories", category);
            }

            FilterExpressionBuilder.Op dateOp = null;
            if (year != null) {
                // submitted_date is stored as a string, so we constrain between start and end of that year
                String startDate = year + "-01-01";
                String endDate = year + "-12-31";
                dateOp = b.and(
                    b.gte("submitted_date", startDate),
                    b.lte("submitted_date", endDate)
                );
            }

            // Combine operators and compile into the final Expression once at the end using build()
            Filter.Expression filterExpression = null;
            if (categoryOp != null && dateOp != null) {
                filterExpression = b.and(categoryOp, dateOp).build();
            } else if (categoryOp != null) {
                filterExpression = categoryOp.build();
            } else if (dateOp != null) {
                filterExpression = dateOp.build();
            }

            VectorSearchRequest searchRequest = new VectorSearchRequest(query, topK, filterExpression);
            List<VectorSearchResult> results = vectorStoreGateway.search(searchRequest);
            
            if (results.isEmpty()) {
                return "No relevant papers matching the specified criteria were found in the database.";
            }

            return fetchAndFormatPapers(results);
        };
    }

    /**
     * Resolves metadata from PostgreSQL and formats it for LLM grounded parsing.
     */
    private String fetchAndFormatPapers(List<VectorSearchResult> results) {
        List<String> paperIds = results.stream().map(VectorSearchResult::paperId).toList();
        List<Paper> papers = paperRepository.findAllByArxivIdIn(paperIds);

        Map<String, Paper> paperMap = papers.stream()
            .collect(Collectors.toMap(Paper::getArxivId, Function.identity()));

        List<Paper> orderedPapers = new ArrayList<>();
        for (VectorSearchResult res : results) {
            Paper p = paperMap.get(res.paperId());
            if (p != null) {
                orderedPapers.add(p);
            }
        }

        if (orderedPapers.isEmpty()) {
            return "Documents were identified but canonical metadata details could not be resolved from PostgreSQL.";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < orderedPapers.size(); i++) {
            Paper p = orderedPapers.get(i);
            sb.append("Document [").append(i + 1).append("]:\n")
              .append("Title: ").append(p.getTitle()).append("\n")
              .append("Abstract: ").append(p.getAbstractText()).append("\n")
              .append("arXiv ID: ").append(p.getArxivId()).append("\n\n");
        }
        return sb.toString();
    }
}
