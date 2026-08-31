package com.example.arxivrag.embedding;

import com.example.arxivrag.paper.Paper;
import com.example.arxivrag.paper.PaperRepository;
import com.example.arxivrag.vector.VectorStoreGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service coordinating the resumable, batch ingestion of PostgreSQL papers into pgvector.
 */
@Service
public class EmbeddingIngestionService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingIngestionService.class);

    private final PaperRepository paperRepository;
    private final EmbeddingCheckpointRepository checkpointRepository;
    private final VectorStoreGateway vectorStoreGateway;

    @Value("${spring.ai.openai.embedding.options.model:text-embedding-3-small}")
    private String embeddingModelName;

    @Value("${rag.ingestion.batch-size:100}")
    private int batchSize = 100;

    @Autowired
    public EmbeddingIngestionService(
            PaperRepository paperRepository,
            EmbeddingCheckpointRepository checkpointRepository,
            VectorStoreGateway vectorStoreGateway) {
        this.paperRepository = paperRepository;
        this.checkpointRepository = checkpointRepository;
        this.vectorStoreGateway = vectorStoreGateway;
    }

    /**
     * Executes a complete ingestion run, scanning and processing all papers in configurable batches.
     */
    @Transactional
    public IngestionResult ingest() {
        log.info("Starting batch paper embedding ingestion using model: {}", embeddingModelName);
        
        int pageNumber = 0;
        int totalProcessed = 0;
        int newlyEmbedded = 0;
        int updated = 0;
        int skipped = 0;
        
        Page<Paper> page;
        do {
            Pageable pageable = PageRequest.of(pageNumber, batchSize);
            page = paperRepository.findAll(pageable);
            
            List<Paper> papers = page.getContent();
            if (papers.isEmpty()) {
                break;
            }
            
            BatchStats batchStats = processBatch(papers);
            newlyEmbedded += batchStats.newlyEmbedded();
            updated += batchStats.updated();
            skipped += batchStats.skipped();
            totalProcessed += papers.size();
            
            pageNumber++;
        } while (page.hasNext());

        IngestionResult result = new IngestionResult(totalProcessed, newlyEmbedded, updated, skipped);
        log.info("Ingestion completed: {}", result);
        return result;
    }

    /**
     * Processes a single batch of papers, applying change detection and updating checkpoints.
     */
    public BatchStats processBatch(List<Paper> papers) {
        List<String> paperIds = papers.stream().map(Paper::getArxivId).toList();
        
        // 1. Fetch checkpoints in bulk for the current page to avoid N+1 query overhead
        List<EmbeddingCheckpointId> checkpointIds = paperIds.stream()
            .map(id -> new EmbeddingCheckpointId(id, "0", embeddingModelName))
            .toList();
        
        List<EmbeddingCheckpoint> existingCheckpoints = checkpointRepository.findAllById(checkpointIds);
        Map<String, EmbeddingCheckpoint> checkpointMap = existingCheckpoints.stream()
            .collect(Collectors.toMap(EmbeddingCheckpoint::getPaperId, Function.identity()));

        List<RetrievalDocument> docsToEmbed = new ArrayList<>();
        List<String> docsToDeleteInVectorDb = new ArrayList<>();
        List<EmbeddingCheckpoint> checkpointsToUpsert = new ArrayList<>();
        
        int skipped = 0;
        int newlyEmbedded = 0;
        int updated = 0;

        for (Paper paper : papers) {
            RetrievalDocument doc = RetrievalDocumentFactory.createDocument(paper);
            EmbeddingCheckpoint checkpoint = checkpointMap.get(paper.getArxivId());

            if (checkpoint == null) {
                // Scenario: New paper
                docsToEmbed.add(doc);
                checkpointsToUpsert.add(new EmbeddingCheckpoint(paper.getArxivId(), "0", embeddingModelName, doc.contentHash()));
                newlyEmbedded++;
            } else if (!checkpoint.getContentHash().equals(doc.contentHash())) {
                // Scenario: Paper text modified in PostgreSQL
                docsToEmbed.add(doc);
                docsToDeleteInVectorDb.add(paper.getArxivId());
                
                checkpoint.setContentHash(doc.contentHash());
                checkpoint.setEmbeddedAt(java.time.OffsetDateTime.now());
                checkpointsToUpsert.add(checkpoint);
                updated++;
            } else {
                // Scenario: Unchanged paper, skip to save tokens
                skipped++;
            }
        }

        // 2. Perform DB synchronization
        if (!docsToDeleteInVectorDb.isEmpty()) {
            vectorStoreGateway.deleteByPaperIds(docsToDeleteInVectorDb);
        }

        if (!docsToEmbed.isEmpty()) {
            vectorStoreGateway.save(docsToEmbed);
            checkpointRepository.saveAll(checkpointsToUpsert);
            log.info("Processed batch: embedded {} new papers, updated {} modified papers, skipped {} unchanged papers", 
                newlyEmbedded, updated, skipped);
        }

        return new BatchStats(newlyEmbedded, updated, skipped);
    }

    // Getters and Setters for configuration tuning
    public String getEmbeddingModelName() {
        return embeddingModelName;
    }

    public void setEmbeddingModelName(String embeddingModelName) {
        this.embeddingModelName = embeddingModelName;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * Statistics for a single batch processing run.
     */
    public record BatchStats(int newlyEmbedded, int updated, int skipped) {}

    /**
     * Comprehensive result representing the overall outcome of the ingestion execution.
     */
    public record IngestionResult(int totalProcessed, int newlyEmbedded, int updated, int skipped) {}
}
