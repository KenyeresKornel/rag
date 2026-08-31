package com.example.arxivrag.embedding;

import com.example.arxivrag.paper.Paper;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RetrievalDocumentFactoryTests {

    @Test
    void testScenario3_1_DocumentFormattingAndMapping() {
        // Arrange: A paper with messy untrimmed whitespace and multiple internal spaces/newlines
        Paper paper = new Paper(
            "2401.12345",
            "   Deep Ingestion \n Systems   ",
            " Ingestion   abstract \t text.\nAnother paragraph. ",
            List.of("Bengio"),
            List.of("cs.CL"),
            LocalDate.of(2024, 1, 17),
            null,
            null
        );

        // Act: Map to RetrievalDocument
        RetrievalDocument doc = RetrievalDocumentFactory.createDocument(paper);

        // Assert: Format and contents match exactly
        String expectedText = "Title: Deep Ingestion Systems\n\nAbstract:\nIngestion abstract text. Another paragraph.";
        assertThat(doc.text()).isEqualTo(expectedText);

        assertThat(doc.documentId()).isEqualTo("arxiv:2401.12345:chunk:0");
        assertThat(doc.paperId()).isEqualTo("2401.12345");
        assertThat(doc.metadata()).containsEntry("paper_id", "2401.12345");
        assertThat(doc.metadata()).containsEntry("chunk_id", "0");
        assertThat(doc.metadata()).containsEntry("categories", List.of("cs.CL"));
        assertThat(doc.metadata()).containsEntry("submitted_date", "2024-01-17");
    }

    @Test
    void testScenario3_2_HashSensitivenessAndStability() {
        Paper paper1 = new Paper(
            "2401.12345",
            "Deep Ingestion Systems",
            "Ingestion abstract text.",
            List.of("Bengio"),
            List.of("cs.CL"),
            LocalDate.of(2024, 1, 17),
            null,
            null
        );

        // Identical text but different whitespace formatting
        Paper paper2 = new Paper(
            "2401.12345",
            "  Deep   Ingestion   Systems  ",
            "\nIngestion   abstract   text.  ",
            List.of("Bengio"),
            List.of("cs.CL"),
            LocalDate.of(2024, 1, 17),
            null,
            null
        );

        // Slightly different text
        Paper paper3 = new Paper(
            "2401.12345",
            "Deep Ingestion Systems",
            "Ingestion abstract text. Modified.",
            List.of("Bengio"),
            List.of("cs.CL"),
            LocalDate.of(2024, 1, 17),
            null,
            null
        );

        RetrievalDocument doc1 = RetrievalDocumentFactory.createDocument(paper1);
        RetrievalDocument doc2 = RetrievalDocumentFactory.createDocument(paper2);
        RetrievalDocument doc3 = RetrievalDocumentFactory.createDocument(paper3);

        // 1. Stability: Re-running twice on the same text returns the exact same hash
        assertThat(doc1.contentHash()).isEqualTo(doc2.contentHash());
        assertThat(doc1.contentHash()).hasSize(64); // SHA-256 standard size in hex

        // 2. Sensitiveness: A slight change in text produces a completely different hash
        assertThat(doc1.contentHash()).isNotEqualTo(doc3.contentHash());
    }
}
