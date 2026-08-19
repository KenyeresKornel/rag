# arxiv-rag

Spring Boot baseline for the arXiv retrieval/RAG project.

## Baseline

- Java 21
- Spring Boot 4.1.0
- Maven 3.9.11, invoked through the Maven Wrapper
- Maven Wrapper 3.3.4

The broader v1 technical contract is documented in [`docs/decisions.md`](docs/decisions.md).

## Prerequisites

- JDK 21
- Internet access on the first wrapper run so Maven can be downloaded

A system Maven installation is not required.

## Build and test

```bash
./mvnw clean verify
```

On Windows:

```bat
mvnw.cmd clean verify
```

## Run

```bash
./mvnw spring-boot:run
```

The baseline exposes Spring Boot Actuator health information at:

```text
GET /actuator/health
```

## Package structure

```text
com.example.arxivrag
├── arxiv       # dataset ingestion/import boundary
├── paper       # canonical paper domain/persistence boundary
├── embedding   # retrieval documents and embedding ingestion
└── vector      # vector-store-neutral retrieval boundary
    └── pgvector
```

Only package boundaries are created by this bootstrap ticket. Domain classes, repositories, importers, migrations, database infrastructure, and vector-store implementations are intentionally deferred to later tickets.

## Repository documentation

- `docs/decisions.md` — frozen v1 technical decisions
- `docs/architecture.md` — architecture/package-boundary skeleton
- `docs/dataset.md` — dataset documentation skeleton

## Next implementation tickets

1. PostgreSQL/Flyway infrastructure and relational paper schema.
2. Streaming arXiv dataset reader and importer.
3. pgvector and embedding ingestion.
4. Semantic retrieval and paper-ID resolution.
