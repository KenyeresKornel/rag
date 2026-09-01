package com.example.arxivrag.chat;

import com.example.arxivrag.embedding.RetrievalDocument;
import com.example.arxivrag.paper.Paper;
import com.example.arxivrag.paper.PaperRepository;
import com.example.arxivrag.vector.VectorStoreGateway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class AgentChatIntegrationTests {

    @Autowired
    private ChatController chatController;

    @Autowired
    private PaperRepository paperRepository;

    @Autowired
    private VectorStoreGateway vectorStoreGateway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM vector_store");
        jdbcTemplate.update("DELETE FROM embedding_checkpoints");
        jdbcTemplate.update("DELETE FROM papers");
    }

    @Test
    void testAgentChatIntegrationWithMockFunctions() {
        // Arrange: Seed test papers
        Paper paper1 = new Paper(
            "doc-agent-1", "Transfer Learning in Deep Models", "Agent details.",
            List.of("Vaswani"), List.of("cs.CL"), LocalDate.of(2024, 1, 1), null, null
        );
        Paper paper2 = new Paper(
            "doc-agent-2", "Semantic Alignment Mechanics", "Agent details.",
            List.of("Amodei"), List.of("cs.CL"), LocalDate.of(2024, 1, 1), null, null
        );
        paperRepository.saveAll(List.of(paper1, paper2));

        // Seed precomputed vector dimensions
        float[] v1 = new float[1536]; v1[0] = 1.0f;
        float[] v2 = new float[1536]; v2[1] = 1.0f;
        
        vectorStoreGateway.save(
            List.of(
                new RetrievalDocument(
                    "arxiv:doc-agent-1:chunk:0", "doc-agent-1", 
                    "Transfer Learning in Deep Models\nAgent details.", 
                    Map.of("paper_id", "doc-agent-1", "chunk_id", "0"), "h1"
                ),
                new RetrievalDocument(
                    "arxiv:doc-agent-2:chunk:0", "doc-agent-2", 
                    "Semantic Alignment Mechanics\nAgent details.", 
                    Map.of("paper_id", "doc-agent-2", "chunk_id", "0"), "h2"
                )
            ),
            List.of(v1, v2)
        );

        // Act: Execute POST /api/chat/agent with a category constraint to trigger hybrid search
        RagChatRequest request = new RagChatRequest("Search papers about deep learning in category cs.CL", 2);
        RagChatResponse response = chatController.agentChat(request);

        // Assert: Verify generated thought-trace and citations
        assertThat(response).isNotNull();
        assertThat(response.responseText()).contains("Agent Thought Trace");
        assertThat(response.responseText()).contains("hybridSearch");
        assertThat(response.responseText()).contains("category");

        // Verify resolved citations
        assertThat(response.citations()).hasSize(2);
        
        Citation cit1 = response.citations().stream()
            .filter(c -> "doc-agent-1".equals(c.arxivId()))
            .findFirst()
            .orElseThrow();
        assertThat(cit1.title()).isEqualTo("Transfer Learning in Deep Models");

        Citation cit2 = response.citations().stream()
            .filter(c -> "doc-agent-2".equals(c.arxivId()))
            .findFirst()
            .orElseThrow();
        assertThat(cit2.title()).isEqualTo("Semantic Alignment Mechanics");
    }
}
