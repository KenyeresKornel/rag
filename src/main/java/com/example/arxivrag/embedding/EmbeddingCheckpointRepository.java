package com.example.arxivrag.embedding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Spring Data JPA Repository for managing {@link EmbeddingCheckpoint} entities.
 */
@Repository
public interface EmbeddingCheckpointRepository extends JpaRepository<EmbeddingCheckpoint, EmbeddingCheckpointId> {

    /**
     * Finds all checkpoints associated with a given paper ID (e.g. arXiv identifier).
     */
    List<EmbeddingCheckpoint> findAllByPaperId(String paperId);
}
