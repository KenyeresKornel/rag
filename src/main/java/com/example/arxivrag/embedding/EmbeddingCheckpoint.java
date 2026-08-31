package com.example.arxivrag.embedding;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Entity representing an embedding checkpoint to ensure resumable, idempotent ingestion runs.
 */
@Entity
@Table(name = "embedding_checkpoints")
@IdClass(EmbeddingCheckpointId.class)
public class EmbeddingCheckpoint {

    @Id
    @Column(name = "paper_id", length = 50, nullable = false)
    private String paperId;

    @Id
    @Column(name = "chunk_id", length = 100, nullable = false)
    private String chunkId;

    @Id
    @Column(name = "embedding_model", length = 100, nullable = false)
    private String embeddingModel;

    @Column(name = "content_hash", length = 64, nullable = false)
    private String contentHash;

    @Column(name = "embedded_at", nullable = false)
    private OffsetDateTime embeddedAt;

    @PrePersist
    protected void onCreate() {
        if (embeddedAt == null) {
            embeddedAt = OffsetDateTime.now();
        }
    }

    public EmbeddingCheckpoint() {
        // Default constructor
    }

    public EmbeddingCheckpoint(String paperId, String chunkId, String embeddingModel, String contentHash) {
        this.paperId = paperId;
        this.chunkId = chunkId;
        this.embeddingModel = embeddingModel;
        this.contentHash = contentHash;
        this.embeddedAt = OffsetDateTime.now();
    }

    public String getPaperId() {
        return paperId;
    }

    public void setPaperId(String paperId) {
        this.paperId = paperId;
    }

    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public OffsetDateTime getEmbeddedAt() {
        return embeddedAt;
    }

    public void setEmbeddedAt(OffsetDateTime embeddedAt) {
        this.embeddedAt = embeddedAt;
    }
}
