# arXiv Dataset Ingestion & Relational Pipeline

The project implements a highly modular, memory-efficient, and fully idempotent streaming ingestion pipeline to import paper metadata from the official arXiv dataset (`arxiv-metadata-oai-snapshot.json`).

---

## 1. Ingestion Overview
Rather than loading the massive 2.7M+ record Kaggle dataset into memory, the importer operates on a line-by-line streaming architecture:
- **Streaming Parser (`ArxivDatasetReader`)**: Reads the dataset as a JSON Lines (JSONL) file, deserializing each line lazily into an intermediate DTO (`ArxivRecord`) using **Jackson 3.0** (`tools.jackson`). This maintains a constant $O(1)$ memory overhead during ingestion.
- **Tolerant Parsing**: If a line is malformed or corrupted, it logs a warning with the exception details and continues streaming the subsequent lines.

---

## 2. Ingestion Constraints & Filtering (`ArxivRecordFilter`)
Records are evaluated on-the-fly based on properties configured in `src/main/resources/application.yml`:
```yaml
arxiv:
  import:
    categories:
      - cs.CL
      - cs.AI
      - cs.LG
      - cs.CV
    from-date: 2022-01-01
    max-records: 50000
```
- **Category Match**: A paper is accepted if ANY of its categories (parsed from space-separated tags, e.g., `"cs.CL cs.AI"`) matches one of the configured categories.
- **Submission Date Match**: Evaluates the creation timestamp of the paper's `v1` version. Truncates time and zone details for consistent, timezone-agnostic comparisons. If `v1` metadata is absent, it falls back to the record's `update_date`. Records with `submitted_date < from-date` are excluded.
- **Record Limits**: Stops parsing and terminates ingestion gracefully once the maximum record ceiling (`max-records`) is hit to conserve space on local developer environments.

---

## 3. Entity Mapping & Normalization (`PaperMapper`)
Each accepted `ArxivRecord` is mapped into a canonical `Paper` database entity with the following normalization rules:
- **Whitespace Trimming**: Standardizes all line-breaks, tabs, and redundant spaces in titles and abstracts into a single space, stripping leading/trailing spacing.
- **Authors & Categories Arrays**: Maps list elements into structured Java `List<String>`, stored natively in PostgreSQL as `text[]` and `varchar(50)[]` respectively.
- **Null Preservation**: Missing optional metadata fields, such as `doi` or `journal_ref`, are preserved strictly as SQL `NULL` instead of empty strings.

---

## 4. Idempotency & Bulk Loading (`ArxivImportService`)
Ingestion is designed to be completely safe to execute multiple times against the same database instance:
- **Batch Processing**: Accumulates accepted records and inserts them in bulk chunks of `1000` records to optimize JDBC transport cost.
- **In-place Upserting**: For each record in a batch, it queries PostgreSQL to find any existing paper with the same `arxiv_id`. If a record exists, the service merges the update by preserving its database primary key (`id`) and its original `created_at` timestamp while refreshing the fields. Otherwise, it persists it as a new entity.

---

## 5. Execution & CLI Trigger
Ingestion is initiated from the command line using a dedicated command-line runner trigger:

```bash
# On Unix:
./mvnw spring-boot:run "-Dspring-boot.run.arguments=import-arxiv /path/to/arxiv-metadata-oai-snapshot.json"

# On Windows (PowerShell):
.\mvnw spring-boot:run "-Dspring-boot.run.arguments=import-arxiv /path/to/arxiv-metadata-oai-snapshot.json"
```

### Statistics Summary
Upon completion, the application prints a formatted summary block to standard output and shuts down the active web context and JVM cleanly with exit code `0`:
```text
arXiv import complete

Records read:                    10
Category matched:                 7
Imported (new):                   7
Updated:                          0
Skipped:                          3
Invalid records:                  0
Elapsed:            0.576 s
```
