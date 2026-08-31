package com.example.arxivrag.arxiv;

import com.example.arxivrag.paper.Paper;
import com.example.arxivrag.paper.PaperRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class ArxivImportTests {

    private ObjectMapper objectMapper;
    private ArxivDatasetReader reader;
    private ArxivRecordFilter filter;
    private PaperMapper mapper;
    private PaperRepository paperRepository;
    private ArxivImportProperties properties;
    private ArxivImportService importService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        reader = new ArxivDatasetReader(objectMapper);
        properties = new ArxivImportProperties();
        
        properties.setCategories(List.of("cs.CL", "cs.AI"));
        properties.setFromDate(LocalDate.of(2022, 1, 1));
        properties.setMaxRecords(50);

        filter = new ArxivRecordFilter(properties);
        mapper = new PaperMapper(filter);
        paperRepository = mock(PaperRepository.class);
        importService = new ArxivImportService(reader, filter, mapper, paperRepository, properties);
    }

    @Test
    void testReaderSkipsMalformedLinesAndStreamsSuccessfully(@TempDir File tempDir) throws Exception {
        String line1 = "{\"id\":\"001\",\"submitter\":\"A\",\"authors\":\"Author 1\",\"title\":\"Title 1\",\"categories\":\"cs.CL\",\"versions\":[{\"version\":\"v1\",\"created\":\"Mon, 3 Jan 2022 10:00:00 GMT\"}],\"update_date\":\"2022-01-03\",\"abstract\":\"Abstract 1\"}";
        String line2 = "{this is malformed json}";
        String line3 = "{\"id\":\"002\",\"submitter\":\"B\",\"authors\":\"Author 2\",\"title\":\"Title 2\",\"categories\":\"cs.AI\",\"versions\":[{\"version\":\"v1\",\"created\":\"Tue, 4 Jan 2022 10:00:00 GMT\"}],\"update_date\":\"2022-01-04\",\"abstract\":\"Abstract 2\"}";
        
        File tempFile = new File(tempDir, "dataset.json");
        Files.write(tempFile.toPath(), List.of(line1, line2, line3));

        List<ArxivRecord> records;
        try (var stream = reader.readRecords(tempFile)) {
            records = stream.toList();
        }

        assertThat(records).hasSize(2);
        assertThat(records.get(0).id()).isEqualTo("001");
        assertThat(records.get(1).id()).isEqualTo("002");
    }

    @Test
    void testFilterInclusionsAndExclusions() {
        ArxivRecord matchRecord = new ArxivRecord("001", "A", "Author 1", "Title 1", "Comment", "Journal", "DOI", "Report", "cs.CL", "License", "Abstract", 
                List.of(new ArxivRecord.Version("v1", "Mon, 3 Jan 2022 10:00:00 GMT")), "2022-01-03");

        ArxivRecord wrongCategoryRecord = new ArxivRecord("002", "A", "Author 1", "Title 1", "Comment", "Journal", "DOI", "Report", "math.CO", "License", "Abstract", 
                List.of(new ArxivRecord.Version("v1", "Mon, 3 Jan 2022 10:00:00 GMT")), "2022-01-03");

        ArxivRecord oldDateRecord = new ArxivRecord("003", "A", "Author 1", "Title 1", "Comment", "Journal", "DOI", "Report", "cs.CL", "License", "Abstract", 
                List.of(new ArxivRecord.Version("v1", "Fri, 31 Dec 2021 10:00:00 GMT")), "2021-12-31");

        assertThat(filter.test(matchRecord)).isTrue();
        assertThat(filter.test(wrongCategoryRecord)).isFalse();
        assertThat(filter.test(oldDateRecord)).isFalse();
    }

    @Test
    void testPaperMapperFillsAllFieldsAndNormalizesWhitespace() {
        ArxivRecord record = new ArxivRecord(
                "001", 
                "A", 
                "Author 1, Author 2", 
                "  Title   with  Whitespace\nNormalization  ", 
                "", 
                "   ", 
                "", 
                "", 
                "cs.CL cs.AI", 
                "", 
                "  Abstract\nText  ", 
                List.of(new ArxivRecord.Version("v1", "Mon, 3 Jan 2022 10:00:00 GMT")), 
                "2022-01-03"
        );

        Paper paper = mapper.map(record);

        assertThat(paper.getArxivId()).isEqualTo("001");
        assertThat(paper.getTitle()).isEqualTo("Title with Whitespace Normalization");
        assertThat(paper.getAbstractText()).isEqualTo("Abstract Text");
        assertThat(paper.getAuthors()).containsExactly("Author 1", "Author 2");
        assertThat(paper.getCategories()).containsExactly("cs.CL", "cs.AI");
        assertThat(paper.getSubmittedDate()).isEqualTo(LocalDate.of(2022, 1, 3));
        assertThat(paper.getDoi()).isNull();
        assertThat(paper.getJournalRef()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testImportDatasetIsIdempotentAndTracksStatsCorrectly(@TempDir File tempDir) throws Exception {
        String record1 = "{\"id\":\"001\",\"submitter\":\"A\",\"authors\":\"Author 1\",\"title\":\"Title 1\",\"categories\":\"cs.CL\",\"versions\":[{\"version\":\"v1\",\"created\":\"Mon, 3 Jan 2022 10:00:00 GMT\"}],\"update_date\":\"2022-01-03\",\"abstract\":\"Abstract 1\"}";
        String record2 = "{\"id\":\"002\",\"submitter\":\"B\",\"authors\":\"Author 2\",\"title\":\"Title 2\",\"categories\":\"cs.AI\",\"versions\":[{\"version\":\"v1\",\"created\":\"Tue, 4 Jan 2022 10:00:00 GMT\"}],\"update_date\":\"2022-01-04\",\"abstract\":\"Abstract 2\"}";
        
        File tempFile = new File(tempDir, "dataset.json");
        Files.write(tempFile.toPath(), List.of(record1, record2));

        Paper existingPaper001 = new Paper("001", "Old Title", "Old Abstract", List.of("Old Author"), List.of("cs.CL"), LocalDate.of(2022, 1, 3), null, null);
        existingPaper001.setId(123L);
        
        when(paperRepository.findAllByArxivIdIn(anyList())).thenAnswer(invocation -> {
            List<String> ids = invocation.getArgument(0);
            if (ids.contains("001")) {
                return List.of(existingPaper001);
            }
            return Collections.emptyList();
        });

        ImportStats stats = importService.importDataset(tempFile);

        assertThat(stats.getRecordsRead()).isEqualTo(2);
        assertThat(stats.getCategoryMatched()).isEqualTo(2);
        assertThat(stats.getImported()).isEqualTo(1); 
        assertThat(stats.getUpdated()).isEqualTo(1); 

        ArgumentCaptor<List<Paper>> captor = ArgumentCaptor.forClass(List.class);
        verify(paperRepository, times(1)).saveAll(captor.capture());
        
        List<Paper> savedPapers = captor.getValue();
        assertThat(savedPapers).hasSize(2);
        
        Paper saved001 = savedPapers.stream().filter(p -> "001".equals(p.getArxivId())).findFirst().get();
        assertThat(saved001.getId()).isEqualTo(123L); 
        assertThat(saved001.getTitle()).isEqualTo("Title 1"); 
        
        Paper saved002 = savedPapers.stream().filter(p -> "002".equals(p.getArxivId())).findFirst().get();
        assertThat(saved002.getId()).isNull(); 
        assertThat(saved002.getTitle()).isEqualTo("Title 2");
    }
}
