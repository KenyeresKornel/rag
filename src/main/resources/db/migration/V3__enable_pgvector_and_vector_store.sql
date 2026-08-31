-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Create Spring AI pgvector store table
CREATE TABLE IF NOT EXISTS vector_store (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    content TEXT,
    metadata JSONB,
    embedding VECTOR(1536)
);

-- Create HNSW index for fast cosine distance similarity searches
CREATE INDEX IF NOT EXISTS vector_store_embedding_idx 
ON vector_store USING hnsw (embedding vector_cosine_ops);

-- Create GIN index on metadata for fast sub-millisecond structured filters (categories, date, etc.)
CREATE INDEX IF NOT EXISTS idx_vector_store_metadata 
ON vector_store USING gin (metadata);
