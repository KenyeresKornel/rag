# Learning Project: RAG Chatbot over arXiv, with a Vector-DB Bake-off

## Goal

Two devs, no active project, primarily Java. Goal is to come out the other
side comfortable with RAG concepts (embeddings, chunking, retrieval,
grounding/citations) and with hands-on operational experience running and
comparing three vector stores — **pgvector**, **Milvus**, and **Chroma** —
while staying mostly inside the JVM so the "new thing to learn" is the
concepts, not also a new language.

End deliverable: a working chatbot with a web frontend, backed by all three
vector stores (swappable), plus a short written comparison of how they
performed and how they felt to operate.

## Dataset

**[arXiv metadata dataset](https://www.kaggle.com/datasets/Cornell-University/arxiv)**
(Cornell University, via Kaggle) — ~2.7M paper records: title, abstract,
authors, categories, submission date, DOI, journal ref, etc.

- Filter down to a manageable slice to start, e.g. `cs.CL` / `cs.AI` / `cs.LG`
  categories → tens of thousands of papers. Full corpus can come later as a
  scaling test.
- Abstracts give real, meaty text for embeddings without needing to fetch
  and parse full PDFs (that's a good stretch goal, not a v1 requirement).
- Rich structured metadata (authors, categories, dates) is what makes the
  hybrid relational+vector use case below possible — this dataset isn't
  just a bag of text, it's text *plus* a relational table, which is the
  whole point.

## Architecture

Two data stores, one chat app:

- **Relational DB (Postgres)**: papers table — `id`, `title`, `authors`,
  `categories`, `submitted_date`, `doi`, `journal_ref`, etc. Normal SQL,
  normal indexes. This is the "facts and filters" store.
- **Vector store (swappable: pgvector / Milvus / Chroma)**: embeddings of
  title+abstract text, for semantic similarity search. This is the
  "meaning and topics" store.
- **App layer**: Spring Boot + Spring AI (or LangChain4j) — both have
  built-in support for chat models, embedding models, and all three target
  vector stores, so the retrieval pipeline code stays the same across the
  three backends and only the store config changes.
- **LLM**: whatever's available (OpenAI/Anthropic/Azure OpenAI) for both
  embeddings and generation.
- **Frontend**: a simple chat web UI (a basic React/Vue page or even just
  Spring's own templating is fine — this isn't a frontend exercise).

## Use cases — why two data stores, not just one

The interesting/"smarter answers" part of this project is that most real
questions need **both** stores, not just semantic search:

1. **Pure semantic** — *"What are some recent approaches to reducing
   hallucination in LLMs?"* → vector search only, no filters needed.

2. **Pure structured** — *"How many papers has author Yoshua Bengio
   published in 2023?"* → this is a SQL `COUNT`/`GROUP BY`, and would
   actually be *wrong* if answered via vector similarity (embeddings are
   bad at counting/aggregation). A naive "always retrieve from vector
   store" chatbot gets this kind of question wrong — that's a good demo
   moment.

3. **Hybrid (the interesting case)** — *"Summarize the main ideas in papers
   about diffusion models published in the last 12 months in cs.CV."*
   This needs a **structured filter** (category = cs.CV, date range) joined
   with a **semantic search** (topic = diffusion models) before the LLM
   ever sees the text. Two ways to build this, worth having them try both:
   - *SQL-first*: filter candidate paper IDs in Postgres by
     category/date, then restrict the vector search to just those IDs
     (pgvector does this trivially since it's the same DB — a good point
     in pgvector's favor; Milvus/Chroma need metadata filtering support in
     the vector store itself, which is a good point of comparison).
   - *Router/agent pattern*: give the chatbot two callable tools — `sql_query`
     and `vector_search` — and let the LLM (via Spring AI / LangChain4j
     function calling) decide which one(s) to call based on the question,
     then combine the results before answering. This is the more realistic
     "enterprise assistant" pattern (structured system-of-record + unstructured
     docs) and is worth building even though it's more work, since it's the
     pattern that generalizes to real client work.

4. **Grounded answers with citations** — every chatbot answer should show
   which papers it pulled from (title + link), sourced from the relational
   table even when retrieval came from the vector store (join vector hit →
   paper ID → Postgres row for display). Forces them to actually wire the
   two stores together rather than keeping them siloed.

5. **Transparency/debug panel** (nice-to-have) — show in the UI what was
   retrieved from where: SQL rows vs. vector hits vs. final prompt sent to
   the LLM. Very useful for them to understand *why* an answer came out a
   certain way, and doubles as a demo aid.

## Vector DB comparison

Same pipeline, three backends, same eval set:

| | pgvector | Milvus | Chroma |
|---|---|---|---|
| Ops complexity | Lowest — it's just Postgres | Highest — needs etcd + MinIO + standalone service | Low — single container |
| Metadata filtering | Trivial (same DB, plain SQL) | Supported but a separate concept to learn (scalar filtering) | Supported, simpler API |
| Scaling story | Vertical; fine to tens of millions of vectors | Built for horizontal scale/sharding | Prototype-oriented |
| What it teaches | "Vector search bolted onto a normal DB" | "Purpose-built vector infra" (HNSW/IVF tuning, index tradeoffs) | "Fastest path to a working demo" |

Benchmark harness should capture: ingestion time, index build time, query
latency (p50/p95) at a couple of `k` values, and answer quality against a
small hand-checked eval set (~30-50 Q&A pairs generated from the abstracts,
spot-checked by hand).

## Phased roadmap

1. Scope the dataset slice, load into Postgres, get familiar with the data.
2. Build the ingestion pipeline: chunk (short here, so mostly 1 doc = 1
   chunk), embed, store — against **pgvector first** (lowest ops overhead
   to get a working end-to-end pipeline).
3. Build the basic RAG chat loop: retrieve → prompt → answer with
   citations. Get this solid before anything else.
4. Add the structured-query path (SQL tool) and the hybrid router pattern.
5. Swap in Milvus, then Chroma, reusing the same pipeline via the
   store-agnostic interface. Docker Compose each one — the setup
   experience itself is half the lesson.
6. Build the benchmark harness and run it against all three.
7. Minimal web chat frontend, wired to whichever backend is currently
   selected (config flag).
8. Write up the comparison: numbers + a paragraph of "which would we
   actually recommend for a client, and when."

## Stretch goals (only if time allows)

- Swap hosted embeddings for a local/open model (e.g. BGE/E5) — adds a
  cost/latency/quality comparison on top of the vector-store one.
- Ingest full paper text (PDF) instead of just abstracts — forces real
  chunking-strategy decisions.
- Hybrid search (BM25 + vector) and/or a reranking step.
- Scale test: full 2.7M-record corpus instead of the category-filtered
  slice.

## Definition of done

- Working chatbot, web frontend, answers grounded with citations.
- All three vector stores runnable via config swap, same pipeline.
- Benchmark results table + short comparison write-up.
- At least one working hybrid (SQL + vector) use case demoed end-to-end.
