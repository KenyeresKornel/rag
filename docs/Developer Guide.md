# arXiv RAG — Developer Guide

## 1. Overview

`arxiv-rag` is a Java learning project for building a Retrieval-Augmented Generation (RAG) chatbot over arXiv paper metadata and abstracts. The long-term goal is to combine relational filtering in PostgreSQL with semantic retrieval and compare multiple vector-store implementations, while keeping most application development inside the JVM.

The intended end state is a web application that can:

- ingest a filtered subset of arXiv metadata;
- store canonical paper metadata in PostgreSQL;
- generate embeddings from paper title + abstract;
- retrieve semantically similar papers;
- combine structured SQL filtering with vector retrieval;
- generate grounded answers with paper citations;
- switch between pgvector, Milvus, and Chroma;
- benchmark vector-store behavior and retrieval performance.

### Current implementation status

The current `main` branch is a **bootstrap/baseline application**, not yet a complete RAG system.

Implemented today:

- Java 21 Spring Boot application;
- Maven Wrapper build;
- Spring MVC/WebMVC support;
- Spring Boot Actuator;
- static frontend shell;
- package boundaries for planned domain areas;
- basic integration tests;
- GitHub Actions CI;
- Dependabot configuration.

Planned but **not yet implemented on `main`**:

- PostgreSQL connectivity;
- relational paper schema;
- Flyway migrations;
- pgvector extension/schema;
- Spring AI runtime integration;
- Azure OpenAI/OpenAI embedding calls;
- arXiv ingestion/import pipeline;
- paper entities and repositories;
- vector-store contracts and concrete search implementation;
- RAG retrieval/generation API;
- Milvus and Chroma adapters;
- Dockerfile / Docker Compose infrastructure;
- benchmarking implementation.

This distinction is important when developing against the project: `docs/decisions.md` and `docs/project-brief.md` describe the intended architecture, while `pom.xml` and `src/` describe what is actually executable today.

---

## 2. Technology Stack

### Implemented stack

| Area | Technology |
|---|---|
| Language | Java 21 |
| Application framework | Spring Boot 4.1.0 |
| Web framework | Spring WebMVC |
| Operational endpoints | Spring Boot Actuator |
| Build | Maven 3.9.x via Maven Wrapper |
| Testing | JUnit 5 / Spring Boot Test / AssertJ |
| Frontend | Static HTML/CSS/JavaScript served by Spring Boot |
| CI | GitHub Actions |
| Dependency updates | Dependabot |

### Planned v1 stack

The project's recorded v1 decisions specify:

| Area | Planned technology |
|---|---|
| AI framework | Spring AI 2.0.0 |
| Primary embedding provider | Azure OpenAI |
| Embedding fallback | OpenAI API |
| Embedding model | `text-embedding-3-small` |
| Embedding dimension | 1536 |
| Vector similarity | Cosine distance |
| Relational database | PostgreSQL 18 |
| Initial vector extension | pgvector 0.8.6 |
| Schema migration | Flyway |
| Additional vector stores | Milvus and Chroma |

These planned components are not yet declared as runtime dependencies in the current `pom.xml`.

---

## 3. Architecture

### 3.1 Current runtime architecture

The executable application is currently simple:

```text
Browser / HTTP client
        |
        v
Spring Boot 4.1 application
        |
        +-- Spring WebMVC
        |     |
        |     +-- Static UI from src/main/resources/static
        |
        +-- Spring Boot Actuator
              |
              +-- /actuator/health
              +-- /actuator/info
```

`ArxivRagApplication` is the Spring Boot entry point and relies on normal component scanning rooted at `com.example.arxivrag`.

There are currently no application REST controllers, services, repositories, entities, database migrations, embedding clients, or vector-search implementations.

### 3.2 Package boundaries

The project already establishes package boundaries for the planned architecture:

```text
com.example.arxivrag
├── ArxivRagApplication.java
├── arxiv/
│   └── package-info.java
├── paper/
│   └── package-info.java
├── embedding/
│   └── package-info.java
└── vector/
    ├── package-info.java
    └── pgvector/
        └── package-info.java
```

Responsibilities:

- `arxiv` — source dataset reading, filtering, mapping, and import;
- `paper` — canonical paper domain model and relational persistence;
- `embedding` — construction of retrieval documents and embedding ingestion;
- `vector` — vector-store-neutral retrieval contracts;
- `vector.pgvector` — pgvector-specific adapter implementation.

When adding implementation classes, keep infrastructure-specific code behind these boundaries instead of coupling domain or retrieval logic directly to a particular vector database.

### 3.3 Target RAG architecture

The project brief describes a two-store application model:

```text
                    +--------------------+
                    |   arXiv metadata   |
                    +---------+----------+
                              |
                              v
                    +--------------------+
                    | Ingestion / import |
                    +----+----------+----+
                         |          |
             metadata    |          | title + abstract
                         v          v
              +----------------+   +------------------+
              |   PostgreSQL   |   | Embedding model  |
              | canonical data |   +--------+---------+
              +-------+--------+            |
                      |                     v
                      |             +------------------+
                      |             |  Vector store    |
                      |             | pgvector/Milvus/ |
                      |             |     Chroma       |
                      |             +--------+---------+
                      |                      |
                      +-----------+----------+
                                  |
                                  v
                         +------------------+
                         | Retrieval / RAG  |
                         | application layer|
                         +--------+---------+
                                  |
                                  v
                         +------------------+
                         |       LLM        |
                         +--------+---------+
                                  |
                                  v
                         Grounded response
                           + citations
```

PostgreSQL is intended to remain the canonical source for structured paper metadata. Vector hits should resolve back to paper IDs so titles, metadata, filters, and citations can be sourced from canonical relational data.

The planned retrieval modes include:

1. **Semantic retrieval** — vector search for topic similarity.
2. **Structured queries** — SQL for counts, dates, authors, categories, and other exact filters.
3. **Hybrid retrieval** — structured candidate filtering combined with vector similarity.
4. **Grounded generation** — answer generation using retrieved paper context with citations.

---

## 4. Project Structure

A developer-oriented view of the current repository:

```text
rag/
├── .github/
│   ├── workflows/
│   │   └── ci.yml                 # Maven build/test CI
│   └── dependabot.yml             # Weekly Maven/Actions update checks
├── .mvn/
│   └── wrapper/                   # Maven Wrapper metadata
├── docs/
│   ├── architecture.md            # Initial architecture boundary notes
│   ├── dataset.md                 # Dataset implementation status
│   ├── decisions.md               # Frozen v1 technology decisions
│   ├── implementation_steps.txt   # Implementation planning notes
│   └── project-brief.md           # Project goals and target architecture
├── markdown/                      # Earlier planning copies
├── src/
│   ├── main/
│   │   ├── java/com/example/arxivrag/
│   │   │   ├── ArxivRagApplication.java
│   │   │   ├── arxiv/
│   │   │   ├── embedding/
│   │   │   ├── paper/
│   │   │   └── vector/
│   │   │       └── pgvector/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── static/
│   │           ├── index.html
│   │           └── assets/
│   │               ├── app.css
│   │               └── app.js
│   └── test/java/com/example/arxivrag/
│       └── ArxivRagApplicationTests.java
├── .editorconfig
├── .gitattributes
├── .gitignore
├── .java-version
├── README.md
├── mvnw
├── mvnw.cmd
└── pom.xml
```

There is currently no `Dockerfile`, `compose.yml`, or `docker-compose.yml` on the analyzed `main` branch.

---

## 5. Local Development Setup

### 5.1 Prerequisites

Required today:

- Git;
- JDK 21;
- internet access for the first Maven Wrapper run.

A system-wide Maven installation is not required.

Check Java:

```bash
java -version
```

The major version should be 21.

### 5.2 Clone the repository

```bash
git clone https://github.com/KenyeresKornel/rag.git
cd rag
```

### 5.3 Build and test

Linux/macOS:

```bash
./mvnw clean verify
```

Windows Command Prompt or PowerShell:

```powershell
.\mvnw.cmd clean verify
```

`verify` is the preferred developer and CI command because it executes the Maven lifecycle through compilation, tests, packaging, and verification.

### 5.4 Run locally

Linux/macOS:

```bash
./mvnw spring-boot:run
```

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

By default Spring Boot will listen on port `8080` unless configuration is overridden externally.

Open:

```text
http://localhost:8080/
```

Health check:

```text
http://localhost:8080/actuator/health
```

### 5.5 Run the packaged application

After a successful package/build:

```bash
./mvnw clean package
java -jar target/arxiv-rag-0.0.1-SNAPSHOT.jar
```

---

## 6. Configuration

Current application configuration is intentionally minimal.

`src/main/resources/application.yml` configures:

```yaml
spring:
  application:
    name: arxiv-rag

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

### Current configurable behavior

- Spring application name: `arxiv-rag`;
- Actuator web exposure: `health` and `info`.

Standard Spring Boot external configuration mechanisms can override values through command-line options, system properties, environment variables, or profile-specific files.

### Not configured yet

The current branch does not define project-specific settings for:

- datasource URL/username/password;
- Flyway;
- pgvector;
- Azure OpenAI;
- OpenAI;
- embedding model/dimension;
- vector-store selection;
- arXiv dataset location;
- dataset profile;
- retrieval `topK`;
- chat/generation model.

When these are implemented, prefer configuration properties and environment-variable backed secrets rather than hard-coded credentials.

---

## 7. Database and Data Model

### 7.1 Current status

There is **no database runtime integration on `main` yet**. The project has no datasource dependency/configuration, migrations, entities, or repositories in the current baseline.

### 7.2 Planned database architecture

The v1 decisions specify:

- PostgreSQL 18;
- pgvector 0.8.6;
- Flyway for all schema changes;
- Hibernate automatic schema creation should be disabled;
- Spring AI automatic pgvector schema initialization should not own the schema.

The relational `papers` table is intended to be the canonical metadata source.

Expected paper metadata from the project brief includes fields such as:

- paper ID;
- title;
- abstract;
- authors;
- categories;
- submitted date;
- DOI;
- journal reference.

The exact schema should be defined by Flyway migrations once the database ticket is implemented.

### 7.3 Planned vector representation

Current v1 decisions specify:

- one paper = one retrieval document initially;
- embedding input = title + abstract;
- model = `text-embedding-3-small`;
- dimension = 1536;
- similarity = cosine distance;
- vector metadata should include `paper_id`, `chunk_id`, `categories`, and `submitted_date`.

### 7.4 Dataset scope

Recorded v1 dataset decisions:

- categories: `cs.CL`, `cs.AI`, `cs.LG`, `cs.CV`;
- minimum submission date: `2022-01-01`;
- profiles:
  - `tiny`: 5,000 matching papers;
  - `dev`: 50,000 matching papers;
  - `scale`: all matching papers;
- default profile: `dev`.

Dataset ingestion is explicitly not implemented in the current bootstrap application.

---

## 8. Docker

### Current status

The analyzed `main` branch does **not** contain a Dockerfile or Docker Compose definition. Therefore the current branch does not provide a repository-supported Docker build/run command.

Do not document commands such as `docker build .` as working project commands until a Dockerfile is committed.

### Intended direction

The project roadmap expects Docker-based operational comparison for pgvector, Milvus, and Chroma. Containerization should eventually cover:

- the Spring Boot application image;
- PostgreSQL + pgvector for the first vector-store implementation;
- Milvus services when that adapter is added;
- Chroma when that adapter is added;
- environment-based runtime configuration.

### Maven Wrapper considerations when adding Docker

If a Dockerfile builds through the Maven Wrapper, it must copy both the wrapper script and wrapper metadata before invoking Maven, for example conceptually:

```text
mvnw
.mvn/wrapper/...
pom.xml
```

On Linux-based builder images, `mvnw` also needs:

- LF line endings;
- executable permission.

The repository's `.gitattributes` already enforces LF for `mvnw`, but the executable Git file mode must also be preserved.

---

## 9. CI/CD

CI is defined in `.github/workflows/ci.yml`.

### Triggers

The workflow runs on:

- every pull request;
- every push to `main`.

### Build environment

- runner: `ubuntu-latest`;
- JDK distribution: Eclipse Temurin;
- Java version: 21;
- Maven dependency caching: enabled through `actions/setup-java`.

### Build command

```bash
./mvnw --batch-mode --no-transfer-progress clean verify
```

Output is piped to `ci-build.log` while preserving the Maven exit status through `set -o pipefail`.

### Published artifacts

The workflow uploads an artifact named:

```text
maven-test-results-and-build-log
```

It contains, when present:

- Surefire reports;
- Failsafe reports;
- `ci-build.log`.

Artifacts are uploaded even when the build fails and are retained for 14 days.

### Permissions

The workflow explicitly uses read-only repository content permission:

```yaml
permissions:
  contents: read
```

### Dependabot

Dependabot checks weekly for updates in:

- Maven dependencies;
- GitHub Actions dependencies.

Each ecosystem has an open pull request limit of 10.

### Current CI caveat: Maven Wrapper executable bit

The CI workflow invokes `./mvnw`. On Ubuntu this requires the executable bit to be stored in Git. If CI fails with:

```text
./mvnw: Permission denied
```

fix the repository file mode and commit it:

```bash
git update-index --chmod=+x mvnw
git commit -m "Make Maven wrapper executable"
git push
```

Verify with:

```bash
git ls-files -s mvnw
```

The mode should normally start with `100755`, not `100644`.

---

## 10. Testing

Tests live under:

```text
src/test/java/com/example/arxivrag/
```

The current `ArxivRagApplicationTests` class starts Spring Boot on a random port.

### Existing tests

#### `contextLoads()`

Verifies that the Spring application context starts successfully.

#### `servesFrontendShell()`

Starts the embedded HTTP server and requests `/`. It asserts that:

- the response status is in the 2xx range;
- the response body contains `arXiv RAG Lab`.

### Run tests

```bash
./mvnw test
```

or the full CI-equivalent verification:

```bash
./mvnw clean verify
```

Windows:

```powershell
.\mvnw.cmd clean verify
```

### Testing gaps to address as features are added

Future implementation should add tests for:

- Flyway migrations and schema constraints;
- arXiv parsing/filtering/import behavior;
- persistence repositories;
- embedding document construction;
- vector-store adapter contract tests;
- semantic retrieval correctness;
- hybrid SQL/vector filtering;
- API request/response contracts;
- grounding and citation behavior;
- external AI provider error handling;
- container/integration tests for database/vector infrastructure;
- benchmark regression checks where practical.

---

## 11. HTTP and API Endpoints

There are currently no project-specific REST API controllers on `main`.

### `GET /`

Purpose: serves the static frontend shell from `src/main/resources/static/index.html`.

Expected result: HTML page titled **arXiv RAG Lab**.

The frontend currently contains placeholder/navigation areas for concepts such as system status, import, papers, search, chat, vector stores, benchmarks, and settings. These should not be interpreted as implemented backend APIs.

### `GET /actuator/health`

Purpose: Spring Boot health endpoint.

Typical healthy response:

```json
{
  "status": "UP"
}
```

### `GET /actuator/info`

Purpose: Spring Boot Actuator information endpoint.

The endpoint is exposed, but the project does not currently define custom `info.*` metadata, so the returned payload may be empty/minimal.

### Static assets

Spring Boot also serves resources below paths such as:

```text
/assets/app.css
/assets/app.js
```

### Planned API surface

The repository does not yet define stable endpoint contracts for import, papers, retrieval, chat, or benchmarking. Those endpoints should be documented from controller code once implemented rather than being invented in advance.

---

## 12. Frontend

The frontend is deliberately lightweight and is packaged directly into the Spring Boot application.

Current files:

```text
src/main/resources/static/
├── index.html
└── assets/
    ├── app.css
    └── app.js
```

There is no separate Node/npm frontend build step.

The shell currently identifies itself as a demo and indicates that backend functionality is pending. Its purpose is to establish navigation and visual structure before the RAG features are connected.

---

## 13. Troubleshooting

### `./mvnw: Permission denied`

Cause: `mvnw` does not have the executable bit in the Git index or working tree.

Temporary local fix:

```bash
chmod +x mvnw
```

Repository fix:

```bash
git update-index --chmod=+x mvnw
git commit -m "Make Maven wrapper executable"
```

This is especially important because GitHub Actions runs on Ubuntu and invokes `./mvnw` directly.

### `./mvnw: not found` in a Linux/Docker environment

Check that:

1. `mvnw` is actually present in the working directory/image;
2. `.mvn/wrapper` has been copied as well;
3. the script uses LF line endings;
4. the Docker build `WORKDIR` is the directory containing `mvnw`;
5. the script is executable.

The repository's `.gitattributes` specifies LF for `mvnw` and CRLF for `mvnw.cmd`.

### Windows command fails with `./mvnw`

Use the Windows wrapper:

```powershell
.\mvnw.cmd clean verify
```

`./mvnw` is intended for Unix-like shells.

### Maven Wrapper cannot download Maven

The first wrapper invocation requires internet access. Verify proxy/firewall configuration if Maven distribution download fails.

### Wrong Java version

Check:

```bash
java -version
```

The project requires Java 21. Ensure `JAVA_HOME` and your `PATH` reference a JDK 21 installation.

### Port 8080 already in use

Run on a different port:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

or when running the packaged JAR:

```bash
java -jar target/arxiv-rag-0.0.1-SNAPSHOT.jar --server.port=8081
```

### `/actuator/health` returns 404

Confirm that:

- you are running the current application configuration;
- `spring-boot-starter-actuator` is present;
- `management.endpoints.web.exposure.include` still contains `health`.

### Database connection examples do not work

Database support has not yet been implemented on `main`. Do not expect datasource properties, Flyway migrations, PostgreSQL, or pgvector to initialize until the relevant infrastructure is added.

### Docker commands fail because `Dockerfile` is missing

The analyzed `main` branch currently has no Dockerfile. Use Docker only after a Dockerfile is added/merged, or explicitly check out the branch containing that work.

### UI loads but application features do nothing

This is expected in the bootstrap state. The frontend is a shell and most RAG/backend functionality is still pending.

---

## 14. Recommended Development Sequence

The repository documentation points to the following practical sequence:

1. Add PostgreSQL/Flyway infrastructure and canonical relational paper schema.
2. Implement the streaming arXiv dataset reader/importer.
3. Add pgvector and embedding ingestion.
4. Add semantic retrieval and paper-ID resolution.
5. Build the RAG answer loop with grounding/citations.
6. Add structured SQL and hybrid retrieval paths.
7. Introduce vector-store-neutral contracts and alternative Milvus/Chroma implementations.
8. Add benchmark tooling and compare stores.
9. Connect the static UI to real backend APIs.

For each step, update this guide from the executable code rather than only from architectural plans.

---

## 15. Development Conventions

### Build through the wrapper

Prefer:

```bash
./mvnw ...
```

instead of relying on a developer's globally installed Maven version.

### Keep schema ownership explicit

Once persistence is implemented:

- Flyway should own schema evolution;
- avoid Hibernate auto-DDL for production schema management;
- avoid allowing Spring AI to silently create/own the pgvector schema.

### Keep vector-store code replaceable

Do not leak pgvector-specific types throughout the application. Retrieval behavior should be expressed behind the `vector` boundary so Milvus and Chroma can later implement the same application-level contract.

### Keep PostgreSQL canonical

Use vector-store metadata primarily to support retrieval. Resolve final paper metadata/citations from PostgreSQL where practical so structured facts have one authoritative source.

### Keep secrets external

When AI providers and database credentials are introduced, use environment variables, secret stores, or external Spring configuration. Never commit credentials or API keys.

---

## 16. Documentation Maintenance

Update this developer guide whenever changes affect:

- Java/Spring/Maven versions;
- package architecture;
- application configuration;
- database schema/migrations;
- Docker or Compose commands;
- CI behavior;
- supported endpoints;
- required environment variables;
- testing strategy;
- local setup prerequisites.

The most important rule is to separate **current behavior** from **planned architecture**. A developer should be able to tell which commands and APIs work on the checked-out branch without reading the roadmap first.