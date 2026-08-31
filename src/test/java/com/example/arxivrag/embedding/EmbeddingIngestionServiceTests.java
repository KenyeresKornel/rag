package com.example.arxivrag.embedding;

import com.example.arxivrag.paper.Paper;
import com.example.arxivrag.paper.PaperRepository;
import com.example.arxivrag.vector.VectorStoreGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmbeddingIngestionServiceTests {

    private PaperRepository paperRepository;
    private EmbeddingCheckpointRepository checkpointRepository;
    private VectorStoreGateway vectorStoreGateway;
    private EmbeddingIngestionService ingestionService;

    @BeforeEach
    void setUp() {
        paperRepository = mock(PaperRepository.class);
        checkpointRepository = mock(EmbeddingCheckpointRepository.class);
        vectorStoreGateway = mock(VectorStoreGateway.class);
        ingestionService = new EmbeddingIngestionService(paperRepository, checkpointRepository, vectorStoreGateway);
        ingestionService.setEmbeddingModelName("text-embedding-3-small");
        ingestionService.setBatchSize(100);
    }

    private Paper createPaper(String arxivId, String title) {
        return new Paper(
            arxivId, title, "Abstract text for " + arxivId,
            List.of("Author"), List.of("cs.CL"), LocalDate.of(2024, 1, 1), null, null
        );
    }

    @Test
    void testScenario5_1_PipelineChunkingAndBatchBoundaries() {
        // Arrange: Generate 250 mock papers
        List<Paper> allPapers = new ArrayList<>();
        for (int i = 1; i <= 250; i++) {
            allPapers.add(createPaper("arxiv-" + i, "Title " + i));
        }

        // Mock paged responses
        List<Paper> batch1 = allPapers.subList(0, 100);
        List<Paper> batch2 = allPapers.subList(100, 200);
        List<Paper> batch3 = allPapers.subList(200, 250);

        Page<Paper> page1 = new PageImpl<>(batch1, Pageable.ofSize(100), 250);
        Page<Paper> page2 = new PageImpl<>(batch2, Pageable.ofSize(100), 250);
        Page<Paper> page3 = new PageImpl<>(batch3, Pageable.ofSize(100), 250);

        // Standard Page behavior requires returning subsequent pages when hasNext() is called
        when(paperRepository.findAll(any(Pageable.class)))
            .thenReturn(page1) // page 0
            .thenReturn(page2) // page 1
            .thenReturn(page3) // page 2
            .thenReturn(new PageImpl<>(Collections.emptyList())); // safety fallback

        // Mock empty checkpoints so everything gets embedded
        when(checkpointRepository.findAllById(any())).thenReturn(Collections.emptyList());

        // Act: Run ingestion
        EmbeddingIngestionService.IngestionResult result = ingestionService.ingest();

        // Assert: Processed total, verify batch boundaries
        assertThat(result.totalProcessed()).isEqualTo(250);
        assertThat(result.newlyEmbedded()).isEqualTo(250);
        assertThat(result.updated()).isEqualTo(0);
        assertThat(result.skipped()).isEqualTo(0);

        // Verify that vectorStoreGateway.save(...) was called exactly 3 times
        ArgumentCaptor<List<RetrievalDocument>> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(vectorStoreGateway, times(3)).save(saveCaptor.capture());

        List<List<RetrievalDocument>> capturedBatches = saveCaptor.getAllValues();
        assertThat(capturedBatches.get(0)).hasSize(100);
        assertThat(capturedBatches.get(1)).hasSize(100);
        assertThat(capturedBatches.get(2)).hasSize(50);

        // Verify checkpoints saved for each batch
        verify(checkpointRepository, times(3)).saveAll(any());
    }

    @Test
    void testScenario5_2_ResumabilityAndSkippingIdempotency() {
        // Arrange: 5 papers, 2 have matching, valid checkpoints
        List<Paper> papers = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            papers.add(createPaper("arxiv-" + i, "Title " + i));
        }

        Page<Paper> page = new PageImpl<>(papers, Pageable.ofSize(100), 5);
        when(paperRepository.findAll(any(Pageable.class)))
            .thenReturn(page)
            .thenReturn(new PageImpl<>(Collections.emptyList()));

        // Calculate expected hashes for the papers to create valid checkpoints
        String hash1 = RetrievalDocumentFactory.createDocument(papers.get(0)).contentHash();
        String hash2 = RetrievalDocumentFactory.createDocument(papers.get(1)).contentHash();

        EmbeddingCheckpoint cp1 = new EmbeddingCheckpoint("arxiv-1", "0", "text-embedding-3-small", hash1);
        EmbeddingCheckpoint cp2 = new EmbeddingCheckpoint("arxiv-2", "0", "text-embedding-3-small", hash2);

        // Return checkpoints for the first 2 papers
        when(checkpointRepository.findAllById(any()))
            .thenReturn(List.of(cp1, cp2));

        // Act: Run Ingestion
        EmbeddingIngestionService.IngestionResult result = ingestionService.ingest();

        // Assert: 2 skipped (matching checkpoints), 3 newly embedded
        assertThat(result.totalProcessed()).isEqualTo(5);
        assertThat(result.newlyEmbedded()).isEqualTo(3);
        assertThat(result.skipped()).isEqualTo(2);
        assertThat(result.updated()).isEqualTo(0);

        // Capture documents saved to vector store
        ArgumentCaptor<List<RetrievalDocument>> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(vectorStoreGateway, times(1)).save(saveCaptor.capture());
        
        List<RetrievalDocument> savedDocs = saveCaptor.getValue();
        assertThat(savedDocs).hasSize(3);
        assertThat(savedDocs.stream().map(RetrievalDocument::paperId)).containsExactlyInAnyOrder("arxiv-3", "arxiv-4", "arxiv-5");

        // Verify that checkpoints are only written for the 3 processed papers
        ArgumentCaptor<List<EmbeddingCheckpoint>> checkpointCaptor = ArgumentCaptor.forClass(List.class);
        verify(checkpointRepository, times(1)).saveAll(checkpointCaptor.capture());
        
        List<EmbeddingCheckpoint> savedCheckpoints = checkpointCaptor.getValue();
        assertThat(savedCheckpoints).hasSize(3);
        assertThat(savedCheckpoints.stream().map(EmbeddingCheckpoint::getPaperId)).containsExactlyInAnyOrder("arxiv-3", "arxiv-4", "arxiv-5");
    }

    @Test
    void testScenario5_3_ChangeDetectionAndReembedding() {
        // Arrange: 1 paper with an existing checkpoint but modified content (different hash)
        Paper paper = createPaper("arxiv-1", "Original Title");
        
        Page<Paper> page = new PageImpl<>(List.of(paper), Pageable.ofSize(100), 1);
        when(paperRepository.findAll(any(Pageable.class)))
            .thenReturn(page)
            .thenReturn(new PageImpl<>(Collections.emptyList()));

        // Outdated checkpoint hash
        EmbeddingCheckpoint oldCheckpoint = new EmbeddingCheckpoint("arxiv-1", "0", "text-embedding-3-small", "outdated-hash-value");
        when(checkpointRepository.findAllById(any())).thenReturn(List.of(oldCheckpoint));

        // Act: Run Ingestion
        EmbeddingIngestionService.IngestionResult result = ingestionService.ingest();

        // Assert: Paper is updated/re-embedded
        assertThat(result.totalProcessed()).isEqualTo(1);
        assertThat(result.newlyEmbedded()).isEqualTo(0);
        assertThat(result.updated()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(0);

        // Verify old vector deleted to prevent duplication
        verify(vectorStoreGateway, times(1)).deleteByPaperIds(List.of("arxiv-1"));

        // Verify new vector saved
        ArgumentCaptor<List<RetrievalDocument>> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(vectorStoreGateway, times(1)).save(saveCaptor.capture());
        assertThat(saveCaptor.getValue().get(0).paperId()).isEqualTo("arxiv-1");

        // Verify checkpoint updated with the new hash
        ArgumentCaptor<List<EmbeddingCheckpoint>> checkpointCaptor = ArgumentCaptor.forClass(List.class);
        verify(checkpointRepository, times(1)).saveAll(checkpointCaptor.capture());
        
        EmbeddingCheckpoint savedCheckpoint = checkpointCaptor.getValue().get(0);
        assertThat(savedCheckpoint.getPaperId()).isEqualTo("arxiv-1");
        assertThat(savedCheckpoint.getContentHash()).isNotEqualTo("outdated-hash-value");
    }

    @Test
    void testScenario5_4_FaultToleranceAndPartialProgressConservation() {
        // Arrange: Mock three separate pages/batches
        List<Paper> batch1 = List.of(createPaper("arxiv-1", "A"));
        List<Paper> batch2 = List.of(createPaper("arxiv-2", "B"));
        List<Paper> batch3 = List.of(createPaper("arxiv-3", "C"));

        Page<Paper> page1 = new PageImpl<>(batch1, Pageable.ofSize(1), 3);
        Page<Paper> page2 = new PageImpl<>(batch2, Pageable.ofSize(1), 3);
        Page<Paper> page3 = new PageImpl<>(batch3, Pageable.ofSize(1), 3);

        when(paperRepository.findAll(any(Pageable.class)))
            .thenReturn(page1)
            .thenReturn(page2)
            .thenReturn(page3);

        when(checkpointRepository.findAllById(any())).thenReturn(Collections.emptyList());

        // Simulate failure on the second batch
        doNothing().when(vectorStoreGateway).save(argThat(list -> {
            if (list == null) return false;
            for (RetrievalDocument doc : list) {
                if ("arxiv-1".equals(doc.paperId())) return true;
            }
            return false;
        }));
        doThrow(new RuntimeException("API Outage Simulation")).when(vectorStoreGateway).save(argThat(list -> {
            if (list == null) return false;
            for (RetrievalDocument doc : list) {
                if ("arxiv-2".equals(doc.paperId())) return true;
            }
            return false;
        }));

        // Act & Assert: Running ingestion must fail with the thrown RuntimeException
        assertThatThrownBy(() -> ingestionService.ingest())
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("API Outage Simulation");

        // Verify Scenario: Partial progress conservation
        // Batch 1 must be saved, but Batch 2 fails and halts before Batch 3 can even be read
        verify(vectorStoreGateway, times(1)).save(argThat(list -> {
            if (list == null) return false;
            for (RetrievalDocument doc : list) {
                if ("arxiv-1".equals(doc.paperId())) return true;
            }
            return false;
        }));
        verify(checkpointRepository, times(1)).saveAll(argThat(list -> {
            if (list == null) return false;
            for (EmbeddingCheckpoint cp : list) {
                if ("arxiv-1".equals(cp.getPaperId())) return true;
            }
            return false;
        }));

        // Ingestion was halted - Batch 3 must never be requested or saved
        verify(vectorStoreGateway, never()).save(argThat(list -> {
            if (list == null) return false;
            for (RetrievalDocument doc : list) {
                if ("arxiv-3".equals(doc.paperId())) return true;
            }
            return false;
        }));
    }
}
