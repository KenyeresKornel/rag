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
  - `V1__create_papers_table.sql`: Creates the `papers` table, unique indexes, and a **GIN (Generalized Inverted Index)** on `categories` arrays to enable sub-millisecond tag-containment queries.
- **Explicit Driver (`FlywayConfig`)**: A custom programmatic configuration bean triggers Flyway migrations explicitly on context startup, ensuring deterministic migration runs.

---

## 3. Deferred Architectural Boundaries

The following boundaries represent the next stages of the project roadmap and remain stubbed/placeholder boundaries:

- **`com.example.arxivrag.embedding`**: Responsible for taking raw abstracts and titles from the relational DB, constructing clean retrieval-documents, invoking AI embedding models (e.g. OpenAI/Azure OpenAI), and preparing payload vectors.
- **`com.example.arxivrag.vector`**: Defines the vector-database-neutral retrieval contracts and search interfaces.
- **`com.example.arxivrag.vector.pgvector`**: Implements the pgvector-specific vector adapter, inserting embeddings and executing cosine distance searches directly inside PostgreSQL.
