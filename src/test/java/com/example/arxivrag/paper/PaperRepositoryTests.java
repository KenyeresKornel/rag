package com.example.arxivrag.paper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class PaperRepositoryTests {

    @Autowired
    private PaperRepository paperRepository;

    @Test
    void canSaveAndRetrievePaperWithArrays() {
        // Arrange
        Paper paper = new Paper(
                "2401.12345",
                "Attention Is All You Need",
                "We propose a new simple network architecture, the Transformer...",
                List.of("Ashish Vaswani", "Noam Shazeer", "Niki Parmar"),
                List.of("cs.CL", "cs.LG"),
                LocalDate.of(2017, 6, 12),
                "10.48550/arXiv.1706.03762",
                "NeurIPS 2017"
        );

        // Act
        Paper savedPaper = paperRepository.save(paper);
        paperRepository.flush();

        // Assert
        assertThat(savedPaper.getId()).isNotNull();

        Optional<Paper> retrievedOpt = paperRepository.findByArxivId("2401.12345");
        assertThat(retrievedOpt).isPresent();

        Paper retrieved = retrievedOpt.get();
        assertThat(retrieved.getTitle()).isEqualTo("Attention Is All You Need");
        assertThat(retrieved.getAbstractText()).isEqualTo("We propose a new simple network architecture, the Transformer...");
        assertThat(retrieved.getAuthors()).containsExactly("Ashish Vaswani", "Noam Shazeer", "Niki Parmar");
        assertThat(retrieved.getCategories()).containsExactly("cs.CL", "cs.LG");
        assertThat(retrieved.getSubmittedDate()).isEqualTo(LocalDate.of(2017, 6, 12));
        assertThat(retrieved.getDoi()).isEqualTo("10.48550/arXiv.1706.03762");
        assertThat(retrieved.getJournalRef()).isEqualTo("NeurIPS 2017");
        assertThat(retrieved.getCreatedAt()).isNotNull();
        assertThat(retrieved.getUpdatedAt()).isNotNull();
    }
}
