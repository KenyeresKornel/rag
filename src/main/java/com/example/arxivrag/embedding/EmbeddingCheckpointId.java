package com.example.arxivrag.embedding;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for the EmbeddingCheckpoint entity.
 */
public class EmbeddingCheckpointId implements Serializable {

    private String paperId;
    private String chunkId;
    private String embeddingModel;

    public EmbeddingCheckpointId() {
        // Default constructor
    }

    public EmbeddingCheckpointId(String paperId, String chunkId, String embeddingModel) {
        this.paperId = paperId;
        this.chunkId = chunkId;
        this.embeddingModel = embeddingModel;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmbeddingCheckpointId that = (EmbeddingCheckpointId) o;
        return Objects.equals(paperId, that.paperId) &&
               Objects.equals(chunkId, that.chunkId) &&
               Objects.equals(embeddingModel, that.embeddingModel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paperId, chunkId, embeddingModel);
    }
}
