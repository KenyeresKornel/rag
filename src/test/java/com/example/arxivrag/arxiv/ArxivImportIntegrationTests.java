package com.example.arxivrag.arxiv;

import com.example.arxivrag.paper.Paper;
import com.example.arxivrag.paper.PaperRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class ArxivImportIntegrationTests {

    @Autowired
    private ArxivImportService importService;

    @Autowired
    private PaperRepository paperRepository;

    @AfterEach
    void cleanUp() {
        paperRepository.deleteAll();
    }

    @Test
    void testEndToEndImportAndIdempotency(@TempDir File tempDir) throws Exception {
        // 1. Arrange: 4 mock lines in JSON Lines format (3 match, 1 mismatch)
        String line1 = "{\"id\":\"int-001\",\"submitter\":\"A\",\"authors\":\"Author One, Author Two\",\"title\":\"Deep Ingestion Systems\",\"categories\":\"cs.CL\",\"versions\":[{\"version\":\"v1\",\"created\":\"Mon, 3 Jan 2022 10:00:00 GMT\"}],\"update_date\":\"2022-01-03\",\"abstract\":\"Ingestion abstract text.\"}";
        String line2 = "{\"id\":\"int-002\",\"submitter\":\"B\",\"authors\":\"Author Three\",\"title\":\"Math Invariance\",\"categories\":\"math.PR\",\"versions\":[{\"version\":\"v1\",\"created\":\"Mon, 3 Jan 2022 10:00:00 GMT\"}],\"update_date\":\"2022-01-03\",\"abstract\":\"Mathematics details.\"}";
        String line3 = "{\"id\":\"int-003\",\"submitter\":\"C\",\"authors\":\"Author Four\",\"title\":\"Advanced Neural Networks\",\"categories\":\"cs.AI\",\"versions\":[{\"version\":\"v1\",\"created\":\"Tue, 4 Jan 2022 10:00:00 GMT\"}],\"update_date\":\"2022-01-04\",\"abstract\":\"Neural nets abstract text.\",\"doi\":\"10.48550/arXiv.2201.00003\"}";
        String line4 = "{\"id\":\"int-004\",\"submitter\":\"D\",\"authors\":\"Author Five\",\"title\":\"Representation Learning\",\"categories\":\"cs.LG\",\"versions\":[{\"version\":\"v1\",\"created\":\"Wed, 5 Jan 2022 10:00:00 GMT\"}],\"update_date\":\"2022-01-05\",\"abstract\":\"Representation abstract text.\",\"journal-ref\":\"ICML 2022\"}";

        File tempFile = new File(tempDir, "int-dataset.json");
        Files.write(tempFile.toPath(), List.of(line1, line2, line3, line4));

        // 2. Act (First run)
        ImportStats statsRun1 = importService.importDataset(tempFile);

        // 3. Assert (First run)
        assertThat(statsRun1.getRecordsRead()).isEqualTo(4);
        assertThat(statsRun1.getCategoryMatched()).isEqualTo(3);
        assertThat(statsRun1.getImported()).isEqualTo(3); 
        assertThat(statsRun1.getUpdated()).isEqualTo(0);
        assertThat(statsRun1.getSkipped()).isEqualTo(1); 

        List<Paper> papers = paperRepository.findAll();
        List<Paper> imported = papers.stream().filter(p -> p.getArxivId().startsWith("int-")).toList();
        assertThat(imported).hasSize(3);

        Optional<Paper> paper001Opt = paperRepository.findByArxivId("int-001");
        assertThat(paper001Opt).isPresent();
        Paper paper001 = paper001Opt.get();
        assertThat(paper001.getTitle()).isEqualTo("Deep Ingestion Systems");
        assertThat(paper001.getAbstractText()).isEqualTo("Ingestion abstract text.");
        assertThat(paper001.getAuthors()).containsExactly("Author One", "Author Two");
        assertThat(paper001.getCategories()).containsExactly("cs.CL");
        assertThat(paper001.getSubmittedDate()).isEqualTo(LocalDate.of(2022, 1, 3));
        assertThat(paper001.getDoi()).isNull();
        assertThat(paper001.getJournalRef()).isNull();

        Optional<Paper> paper003Opt = paperRepository.findByArxivId("int-003");
        assertThat(paper003Opt).isPresent();
        Paper paper003 = paper003Opt.get();
        assertThat(paper003.getDoi()).isEqualTo("10.48550/arXiv.2201.00003");

        Optional<Paper> paper004Opt = paperRepository.findByArxivId("int-004");
        assertThat(paper004Opt).isPresent();
        Paper paper004 = paper004Opt.get();
        assertThat(paper004.getJournalRef()).isEqualTo("ICML 2022");

        // 4. Act (Second run - Idempotency Check)
        String updatedLine1 = "{\"id\":\"int-001\",\"submitter\":\"A\",\"authors\":\"Author One, Author Two\",\"title\":\"Deep Ingestion Systems v2\",\"categories\":\"cs.CL\",\"versions\":[{\"version\":\"v1\",\"created\":\"Mon, 3 Jan 2022 10:00:00 GMT\"}],\"update_date\":\"2022-01-03\",\"abstract\":\"Ingestion abstract text.\"}";
        Files.write(tempFile.toPath(), List.of(updatedLine1, line2, line3, line4));

        ImportStats statsRun2 = importService.importDataset(tempFile);

        // 5. Assert (Second run)
        assertThat(statsRun2.getRecordsRead()).isEqualTo(4);
        assertThat(statsRun2.getCategoryMatched()).isEqualTo(3);
        assertThat(statsRun2.getImported()).isEqualTo(0); 
        assertThat(statsRun2.getUpdated()).isEqualTo(3);  
        assertThat(statsRun2.getSkipped()).isEqualTo(1);

        List<Paper> papersAfterRun2 = paperRepository.findAll().stream().filter(p -> p.getArxivId().startsWith("int-")).toList();
        assertThat(papersAfterRun2).hasSize(3); 

        Paper paper001Updated = paperRepository.findByArxivId("int-001").get();
        assertThat(paper001Updated.getTitle()).isEqualTo("Deep Ingestion Systems v2"); 
        assertThat(paper001Updated.getId()).isEqualTo(paper001.getId()); 
    }
}
