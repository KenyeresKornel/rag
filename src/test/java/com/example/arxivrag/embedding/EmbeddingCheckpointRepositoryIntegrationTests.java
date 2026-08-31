package com.example.arxivrag.embedding;

import com.example.arxivrag.paper.Paper;
import com.example.arxivrag.paper.PaperRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class EmbeddingCheckpointRepositoryIntegrationTests {

    @Autowired
    private EmbeddingCheckpointRepository checkpointRepository;

    @Autowired
    private PaperRepository paperRepository;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        checkpointRepository.deleteAll();
        paperRepository.deleteAll();
    }

    @Test
    void testScenario3_3_IngestionProgressPersistence() {
        // 1. Save Paper to satisfy the foreign key constraint
        Paper paper = new Paper(
            "check-001",
            "Relational Integrity Checks",
            "Abstract text.",
            List.of("Bengio"),
            List.of("cs.CL"),
            LocalDate.of(2024, 1, 1),
            null,
            null
        );
        paperRepository.save(paper);

        // 2. Create and Save Checkpoint
        EmbeddingCheckpoint checkpoint = new EmbeddingCheckpoint(
            "check-001",
            "chunk:0",
            "text-embedding-3-small",
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        );
        checkpointRepository.save(checkpoint);

        // 3. Find by composite ID
        EmbeddingCheckpointId key = new EmbeddingCheckpointId("check-001", "chunk:0", "text-embedding-3-small");
        Optional<EmbeddingCheckpoint> foundOpt = checkpointRepository.findById(key);
        assertThat(foundOpt).isPresent();
        EmbeddingCheckpoint found = foundOpt.get();
        assertThat(found.getContentHash()).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        assertThat(found.getEmbeddedAt()).isNotNull();

        // 4. Update the record
        found.setContentHash("modified-hash-value-5678");
        checkpointRepository.save(found);

        EmbeddingCheckpoint updated = checkpointRepository.findById(key).get();
        assertThat(updated.getContentHash()).isEqualTo("modified-hash-value-5678");

        // 5. Query by paper
        List<EmbeddingCheckpoint> list = checkpointRepository.findAllByPaperId("check-001");
        assertThat(list).hasSize(1);

        // 6. Delete
        checkpointRepository.delete(updated);
        assertThat(checkpointRepository.findById(key)).isEmpty();
    }
}
