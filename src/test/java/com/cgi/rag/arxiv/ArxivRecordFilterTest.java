package com.cgi.rag.arxiv;

import com.cgi.rag.config.ArxivImportProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArxivRecordFilterTest {

    private final ArxivRecordFilter filter = new ArxivRecordFilter(
            new ArxivImportProperties(List.of("cs.CL", "cs.AI", "cs.LG"), 50_000)
    );

    @Test
    void matchesSingleIncludedCategory() {
        assertThat(filter.matches(new ArxivRecord(List.of("cs.CL")))).isTrue();
    }

    @Test
    void matchesWhenAnyCategoryIsIncluded() {
        assertThat(filter.matches(new ArxivRecord(List.of("cs.CV", "cs.LG", "stat.ML")))).isTrue();
    }

    @Test
    void rejectsWhenNoCategoryMatches() {
        assertThat(filter.matches(new ArxivRecord(List.of("cs.CV", "stat.ML")))).isFalse();
    }

    @Test
    void rejectsEmptyCategories() {
        assertThat(filter.matches(new ArxivRecord(List.of()))).isFalse();
    }

    @Test
    void usesExactMatchingRatherThanSubstringMatching() {
        assertThat(filter.matches(new ArxivRecord(List.of("xcs.CLx")))).isFalse();
    }
}
