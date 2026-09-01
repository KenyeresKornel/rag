package com.example.arxivrag.chat.tools;

import com.example.arxivrag.paper.Paper;
import com.example.arxivrag.paper.PaperRepository;
import com.example.arxivrag.vector.VectorSearchRequest;
import com.example.arxivrag.vector.VectorSearchResult;
import com.example.arxivrag.vector.VectorStoreGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SearchToolsTests {

    private VectorStoreGateway vectorStoreGateway;
    private PaperRepository paperRepository;
    private SearchToolsConfig searchToolsConfig;

    @BeforeEach
    void setUp() {
        vectorStoreGateway = mock(VectorStoreGateway.class);
        paperRepository = mock(PaperRepository.class);
        searchToolsConfig = new SearchToolsConfig(vectorStoreGateway, paperRepository);
    }

    @Test
    void testSemanticSearchToolFunction() {
        // Arrange
        Function<SearchToolsConfig.SemanticSearchInput, String> tool = searchToolsConfig.semanticSearch();
        
        when(vectorStoreGateway.search(any(VectorSearchRequest.class)))
            .thenReturn(List.of(new VectorSearchResult("arxiv-1", "0", 0.95)));
        
        when(paperRepository.findAllByArxivIdIn(any()))
            .thenReturn(List.of(new Paper(
                "arxiv-1", "A", "B", List.of("Auth"), List.of("C"), LocalDate.now(), null, null
            )));

        // Act
        String result = tool.apply(new SearchToolsConfig.SemanticSearchInput("concepts", 2));

        // Assert
        assertThat(result).contains("Document [1]:");
        assertThat(result).contains("Title: A");
        assertThat(result).contains("arXiv ID: arxiv-1");

        ArgumentCaptor<VectorSearchRequest> requestCaptor = ArgumentCaptor.forClass(VectorSearchRequest.class);
        verify(vectorStoreGateway, times(1)).search(requestCaptor.capture());
        
        assertThat(requestCaptor.getValue().query()).isEqualTo("concepts");
        assertThat(requestCaptor.getValue().topK()).isEqualTo(2);
    }

    @Test
    void testHybridSearchToolFunctionExpressionBuilding() {
        // Arrange
        Function<SearchToolsConfig.HybridSearchInput, String> tool = searchToolsConfig.hybridSearch();
        when(vectorStoreGateway.search(any(VectorSearchRequest.class))).thenReturn(Collections.emptyList());

        // Act
        tool.apply(new SearchToolsConfig.HybridSearchInput("concepts", "cs.CL", 2024, 3));

        // Assert
        ArgumentCaptor<VectorSearchRequest> requestCaptor = ArgumentCaptor.forClass(VectorSearchRequest.class);
        verify(vectorStoreGateway, times(1)).search(requestCaptor.capture());
        
        VectorSearchRequest captured = requestCaptor.getValue();
        assertThat(captured.query()).isEqualTo("concepts");
        assertThat(captured.topK()).isEqualTo(3);
        assertThat(captured.filterExpression()).isNotNull();
        
        // Confirm Spring AI Filter Expression correctly parsed category and date boundary filters for 2024!
        String expressionString = captured.filterExpression().toString();
        assertThat(expressionString).contains("cs.CL");
        assertThat(expressionString).contains("2024-01-01");
        assertThat(expressionString).contains("2024-12-31");
    }
}
