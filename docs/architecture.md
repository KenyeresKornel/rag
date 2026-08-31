# System Architecture & Package Structure

This document describes the architectural boundaries and packages implemented in the arXiv RAG project.

---

## 1. Relational Ingestion & Domain Structure

The core system consists of two primary operational domains that manage relational metadata ingestion:

### 1.1 Canonical Paper Domain (`com.example.arxivrag.paper`)
Manages the canonical domain models and relational database mapping of academic papers.
- **`Paper` (JPA Entity)**: Maps academic metadata (such as titles, abstracts, authors, and categories) to the database. Categories and authors are modeled as Java `List<String>` and saved directly to the database as native PostgreSQL arrays.
- **`PaperRepository` (JPA Repository)**: Handles batch saves and searches, including bulk checks via `findAllByArxivIdIn` to support idempotent insertions.

### 1.2 arXiv Dataset Ingestion Boundary (`com.example.arxivrag.arxiv`)
Orchestrates reading, filtering, mapping, and importing files from the external arXiv dataset.
- **`ArxivRecord` (DTO)**: Captures raw, deserialized JSON data from the source file. Features version history mapping to extract version-specific submission dates.
- **`ArxivDatasetReader`**: Streams individual JSON lines lazily into `ArxivRecord` entities using Jackson 3.0, ensuring a constant memory profile.
- **`ArxivRecordFilter`**: Tests records against application configurations (such as category containment and date ranges).
- **`PaperMapper`**: Standardizes whitespace in titles and abstracts, parses comma-separated lists, and normalizes optional empty strings into SQL `NULL`s.
- **`ArxivImportService`**: Batches mappings and executes safe, idempotent database upserts.
- **`ArxivImportCommandLineRunner`**: A Spring Boot application trigger that listens to JVM CLI arguments. If it detects `import-arxiv <file-path>`, it runs the ingestion pipeline and forces a clean exit.

---

## 2. Infrastructure & Database Migrations

- **Database Container (`docker-compose.yml`)**: Uses the official `pgvector/pgvector:pg18` image to spin up PostgreSQL 18 with pre-installed pgvector (0.8.6) capabilities. Persistent data is mapped to a docker volume.
- **Schema Control (Flyway)**: Database schemas are fully controlled via Flyway migrations located under `src/main/resources/db/migration/`.
  - `V1__create_papers_table.sql`: Creates the canonical `papers` table, unique indexes, and a **GIN (Generalized Inverted Index)** on `categories` arrays to enable sub-millisecond tag-containment queries.
  - `V2__create_embedding_checkpoints_table.sql`: Creates the `embedding_checkpoints` progress table with a composite primary key and cascade foreign key constraints.
  - `V3__enable_pgvector_and_vector_store.sql`: Registers pgvector, creates the standard `vector_store` table, builds an **HNSW similarity search index** on the vector column (`vector_cosine_ops`), and a GIN index on metadata JSONB.
- **Explicit Driver (`FlywayConfig`)**: A custom programmatic configuration bean triggers Flyway migrations explicitly on context startup, ensuring deterministic migration runs.

---

## 3. Ingestion & Retrieval Boundaries

All domain boundaries have been fully implemented and verified:

### 3.1 Embedding & Checkpoint Domain (`com.example.arxivrag.embedding`)
Orchestrates creating retrieval documents, computing stable SHA-256 content hashes, tracking ingestion status, and running batch operations.
- **`RetrievalDocument` (DTO)**: Represents the single normalized chunk (Title + Abstract) per paper.
- **`RetrievalDocumentFactory`**: Standardizes whitespace sequences, formats the text payload, and computes the stable content hash.
- **`EmbeddingCheckpoint` & `EmbeddingCheckpointRepository`**: Maps progress milestones to PostgreSQL, tracking which papers have been embedded with which model and hash.
- **`EmbeddingIngestionService`**: Coordinates the pipeline by streaming paper pages, querying checkpoints in bulk to prevent N+1 queries, running change-detection, and executing save operations.

### 3.2 Agnostic Vector Store Domain (`com.example.arxivrag.vector`)
Enforces strict decoupling boundaries so the application never references database-specific clients directly.
- **`VectorSearchRequest`**: Model encapsulating high-level text query searches or low-level raw embedding vectors with custom limits (`topK`).
- **`VectorSearchResult`**: Represents matched items with their score and identifiers.
- **`VectorStoreGateway`**: Database-neutral interface specifying bulk vector insertions, semantic searches, and target deletions.

### 3.3 PostgreSQL pgvector Adapter (`com.example.arxivrag.vector.pgvector`)
Implements the abstract boundaries inside PostgreSQL pgvector.
- **`PgVectorStoreGateway`**: Implements `VectorStoreGateway` wrapping Spring AI's `VectorStore` (specifically the `PgVectorStore` engine). Uses direct, high-performance `JdbcTemplate` operations for pre-computed vector saving, fast deletions, and early empty-table optimizations to bypass external API calls during smoke tests.

---

## 4. Deferred Architectural Boundaries

The following boundaries represent the next stages of the project roadmap and remain deferred:

- **Milvus / Chroma Adapters**: Multi-store selection and comparisons.
- **Grounded Answer Generation Service**: Combining semantic retrieval with LLMs to output Citations.
- **Vector-store Ingestion Benchmarking**: Performance and accuracy analysis under high-scale loads.
