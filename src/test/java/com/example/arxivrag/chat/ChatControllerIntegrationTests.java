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
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class ChatControllerIntegrationTests {

    @Autowired
    private ChatController chatController;

    @Autowired
    private PaperRepository paperRepository;

    @Autowired
    private VectorStoreGateway vectorStoreGateway;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        vectorStoreGateway.deleteByPaperIds(List.of("doc-chat-1", "doc-chat-2"));
        paperRepository.deleteAll();
    }

    @Test
    void testChatControllerIntegrationWithMockModel() {
        // Arrange: Seed canonical papers in PostgreSQL
        Paper paper1 = new Paper(
            "doc-chat-1", "Self-Correction in Language Models", "Fidelity abstracts.",
            List.of("Vaswani"), List.of("cs.CL"), LocalDate.of(2024, 1, 1), null, null
        );
        Paper paper2 = new Paper(
            "doc-chat-2", "Constitutional AI Techniques", "Grounded prompt checks.",
            List.of("Amodei"), List.of("cs.CL"), LocalDate.of(2024, 1, 1), null, null
        );
        paperRepository.saveAll(List.of(paper1, paper2));

        // Seed precomputed vector dimensions so our similarity queries retrieve them deterministic
        float[] v1 = new float[1536]; v1[0] = 1.0f;
        float[] v2 = new float[1536]; v2[1] = 1.0f;
        
        vectorStoreGateway.save(
            List.of(
                new RetrievalDocument(
                    "arxiv:doc-chat-1:chunk:0", "doc-chat-1", 
                    "Self-Correction in Language Models\nFidelity abstracts.", 
                    Map.of("paper_id", "doc-chat-1", "chunk_id", "0"), "h1"
                ),
                new RetrievalDocument(
                    "arxiv:doc-chat-2:chunk:0", "doc-chat-2", 
                    "Constitutional AI Techniques\nGrounded prompt checks.", 
                    Map.of("paper_id", "doc-chat-2", "chunk_id", "0"), "h2"
                )
            ),
            List.of(v1, v2)
        );

        RagChatRequest request = new RagChatRequest("Explain self-correction and alignment", 2);

        // Act: Directly execute controller endpoint
        RagChatResponse response = chatController.chat(request);

        // Assert: Verify response values
        assertThat(response).isNotNull();
        assertThat(response.responseText()).contains("[1]");
        assertThat(response.responseText()).contains("[2]");

        assertThat(response.citations()).hasSize(2);
        
        Citation cit1 = response.citations().get(0);
        assertThat(cit1.arxivId()).isEqualTo("doc-chat-1");
        assertThat(cit1.title()).isEqualTo("Self-Correction in Language Models");
        assertThat(cit1.url()).isEqualTo("https://arxiv.org/abs/doc-chat-1");

        Citation cit2 = response.citations().get(1);
        assertThat(cit2.arxivId()).isEqualTo("doc-chat-2");
        assertThat(cit2.title()).isEqualTo("Constitutional AI Techniques");
        assertThat(cit2.url()).isEqualTo("https://arxiv.org/abs/doc-chat-2");
    }
}
