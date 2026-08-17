package com.cgi.rag.arxiv;

import java.util.List;

/**
 * Minimal record shape needed for Step 1.1 filtering.
 * The full arXiv source model will be defined in the next implementation step.
 */
public record ArxivRecord(List<String> categories) {
}
