package com.example.arxivrag.chat;

import com.example.arxivrag.paper.Paper;
import com.example.arxivrag.paper.PaperRepository;
import com.example.arxivrag.vector.VectorSearchRequest;
import com.example.arxivrag.vector.VectorSearchResult;
import com.example.arxivrag.vector.VectorStoreGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RagChatServiceTests {

    private VectorStoreGateway vectorStoreGateway;
    private PaperRepository paperRepository;
    private ChatModel chatModel;
    private RagChatService ragChatService;

    @BeforeEach
    void setUp() {
        vectorStoreGateway = mock(VectorStoreGateway.class);
        paperRepository = mock(PaperRepository.class);
        chatModel = mock(ChatModel.class);
        ragChatService = new RagChatService(vectorStoreGateway, paperRepository, chatModel);
    }

    private Paper createPaper(String arxivId, String title) {
        return new Paper(
            arxivId, title, "Abstract text for " + title,
            List.of("Author"), List.of("cs.CL"), LocalDate.of(2024, 1, 1), null, null
        );
    }

    @Test
    void testPromptFormattingAndCitationExtraction() {
        // Arrange: Prepare papers, mock search retrieval and database resolution
        Paper p1 = createPaper("arxiv-1", "Quantum Computing Insights");
        Paper p2 = createPaper("arxiv-2", "Modern Transformer Architectures");

        when(vectorStoreGateway.search(any(VectorSearchRequest.class)))
            .thenReturn(List.of(
                new VectorSearchResult("arxiv-1", "0", 0.98),
                new VectorSearchResult("arxiv-2", "0", 0.88)
            ));

        when(paperRepository.findAllByArxivIdIn(anyList()))
            .thenReturn(List.of(p1, p2));

        // Return a mock chat response citing document [1] and [2], but also includes an out-of-bounds citation [99]
        String mockLlmAnswer = "We observed significant speedups in quantum mechanics [1] and great sequential attention efficiency [2]. Let's ignore out-of-bounds [99].";
        AssistantMessage assistantMessage = new AssistantMessage(mockLlmAnswer);
        org.springframework.ai.chat.model.ChatResponse mockChatResponse = new org.springframework.ai.chat.model.ChatResponse(List.of(new org.springframework.ai.chat.model.Generation(assistantMessage)));
        
        when(chatModel.call(any(Prompt.class))).thenReturn(mockChatResponse);

        // Act: Invoke service RAG loop
        RagChatResponse result = ragChatService.chat(new RagChatRequest("explain transformers and quantum", 5));

        // Assert: Markdown response matches LLM output
        assertThat(result.responseText()).isEqualTo(mockLlmAnswer);

        // Assert: Citations successfully parsed, resolved, and attached
        // Out-of-bounds citation [99] must be skipped, only [1] and [2] resolved
        assertThat(result.citations()).hasSize(2);

        Citation citation1 = result.citations().stream()
            .filter(c -> "arxiv-1".equals(c.arxivId()))
            .findFirst()
            .orElseThrow();
        assertThat(citation1.title()).isEqualTo("Quantum Computing Insights");
        assertThat(citation1.url()).isEqualTo("https://arxiv.org/abs/arxiv-1");

        Citation citation2 = result.citations().stream()
            .filter(c -> "arxiv-2".equals(c.arxivId()))
            .findFirst()
            .orElseThrow();
        assertThat(citation2.title()).isEqualTo("Modern Transformer Architectures");
        assertThat(citation2.url()).isEqualTo("https://arxiv.org/abs/arxiv-2");

        // Assert: Verify prompt layout (context papers correctly enumerated)
        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel, times(1)).call(promptCaptor.capture());

        String systemPromptText = promptCaptor.getValue().getInstructions().get(0).getText();
        assertThat(systemPromptText).contains("Document [1]:");
        assertThat(systemPromptText).contains("Title: Quantum Computing Insights");
        assertThat(systemPromptText).contains("Document [2]:");
        assertThat(systemPromptText).contains("Title: Modern Transformer Architectures");
        assertThat(systemPromptText).contains("User Question: explain transformers and quantum");
    }

    @Test
    void testEmptyVectorStoreGracefulHandling() {
        // Arrange: Mock no matches
        when(vectorStoreGateway.search(any())).thenReturn(Collections.emptyList());

        // Act: Invoke chat
        RagChatResponse result = ragChatService.chat(new RagChatRequest("unindexed query", 5));

        // Assert: Returns empty citation and graceful status
        assertThat(result.responseText()).contains("No semantically relevant papers were found");
        assertThat(result.citations()).isEmpty();
    }
}
