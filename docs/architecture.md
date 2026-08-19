# Architecture

This document is intentionally skeletal for the bootstrap ticket.

## Package boundaries

- `paper` — canonical paper domain and relational persistence.
- `arxiv` — arXiv dataset reading, filtering, mapping, and import.
- `embedding` — retrieval-document construction and embedding ingestion.
- `vector` — vector-store-neutral search contracts.
- `vector.pgvector` — pgvector-specific adapter implementation.

The concrete database, importer, embedding, and retrieval implementations belong to later tickets.
