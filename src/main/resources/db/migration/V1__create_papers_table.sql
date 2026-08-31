CREATE TABLE papers (
    id BIGSERIAL PRIMARY KEY,
    arxiv_id VARCHAR(50) UNIQUE NOT NULL,
    title TEXT NOT NULL,
    abstract_text TEXT NOT NULL,
    authors TEXT[] NOT NULL,
    categories VARCHAR(50)[] NOT NULL,
    submitted_date DATE NOT NULL,
    doi VARCHAR(100),
    journal_ref TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Indexes for efficient querying and search
CREATE INDEX idx_papers_arxiv_id ON papers(arxiv_id);
CREATE INDEX idx_papers_submitted_date ON papers(submitted_date);
CREATE INDEX idx_papers_categories ON papers USING gin (categories);
