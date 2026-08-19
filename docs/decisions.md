# V1 Technical Decisions

Java version:
21

Build system:
Maven 3.9.x
Use Maven Wrapper (`./mvnw`) for all builds.

Spring Boot:
4.1.0

Spring AI:
2.0.0

Embedding provider:
Azure OpenAI
Fallback: OpenAI API

Embedding model:
text-embedding-3-small

Embedding dimension:
1536

Vector similarity:
Cosine distance

PostgreSQL:
18

pgvector:
0.8.6

Database migrations:
Flyway

Schema ownership:
Flyway owns all PostgreSQL schema changes.
Disable automatic Hibernate schema creation.
Do not rely on Spring AI automatic pgvector schema initialization.

Initial arXiv categories:
cs.CL
cs.AI
cs.LG
cs.CV

Initial date range:
submitted_date >= 2022-01-01

Dataset profiles:
tiny = 5,000 matching papers
dev = 50,000 matching papers
scale = all matching papers

Default profile:
dev

Initial document strategy:
1 paper = 1 retrieval document

Embedding text:
Title: <title>

Abstract:
<abstract>

Vector metadata:
paper_id
chunk_id
categories
submitted_date

Canonical metadata source:
PostgreSQL papers table