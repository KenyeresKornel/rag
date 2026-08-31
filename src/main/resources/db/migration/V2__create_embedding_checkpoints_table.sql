CREATE TABLE embedding_checkpoints (
    paper_id VARCHAR(50) NOT NULL,
    chunk_id VARCHAR(100) NOT NULL,
    embedding_model VARCHAR(100) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    embedded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (paper_id, chunk_id, embedding_model),
    CONSTRAINT fk_embedding_checkpoints_papers FOREIGN KEY (paper_id) REFERENCES papers(arxiv_id) ON DELETE CASCADE
);

-- Index for quickly checking and filtering already embedded papers
CREATE INDEX idx_embedding_checkpoints_paper ON embedding_checkpoints(paper_id);
