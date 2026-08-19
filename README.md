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

The initial UI shell is served by Spring Boot static resources at:

```text
GET /
```

Frontend assets live under `src/main/resources/static` and are packaged into the backend artifact by Maven's standard resource handling. There is intentionally no separate frontend build step yet.

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

## UI shell

The app shell includes navigation and placeholder pages for:

- System Status
- Import
- Papers
- Search
- Chat
- Vector Stores
- Benchmarks
- Settings

## Repository documentation

- `docs/decisions.md` — frozen v1 technical decisions
- `docs/architecture.md` — architecture/package-boundary skeleton
- `docs/dataset.md` — dataset documentation skeleton

## Next implementation tickets

1. PostgreSQL/Flyway infrastructure and relational paper schema.
2. Streaming arXiv dataset reader and importer.
3. pgvector and embedding ingestion.
4. Semantic retrieval and paper-ID resolution.
