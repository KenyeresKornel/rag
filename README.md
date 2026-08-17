# RAG Learning Project

Target local project root:

```text
C:\Workspace\RAG\RAG
```

## Implemented: Step 1.1 - exact v1 arXiv slice

The dataset slice is externalized in `src/main/resources/application.yml`:

```yaml
arxiv:
  import:
    categories:
      - cs.CL
      - cs.AI
      - cs.LG
    max-records: 50000
```

Rules:

- A paper matches when **any** paper category exactly equals a configured category.
- There is no date restriction in v1.
- `max-records` limits matching/accepted papers, not source records scanned.
- `max-records: null` means unlimited.
- `max-records` must otherwise be greater than zero.

To remove the limit, change only configuration:

```yaml
max-records: null
```

No Java change is required.

## Run tests

From `C:\Workspace\RAG\RAG`:

```powershell
mvn test
```

The next step should define the full arXiv source record model and streaming reader; this step intentionally contains only the minimal `ArxivRecord` shape needed to validate category filtering.
