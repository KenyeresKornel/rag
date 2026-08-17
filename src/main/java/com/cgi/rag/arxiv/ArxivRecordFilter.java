package com.cgi.rag.arxiv;

import com.cgi.rag.config.ArxivImportProperties;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class ArxivRecordFilter {

    private final Set<String> includedCategories;

    public ArxivRecordFilter(ArxivImportProperties properties) {
        this.includedCategories = Set.copyOf(properties.categories());
    }

    public boolean matches(ArxivRecord record) {
        if (record == null) {
            return false;
        }
        return matchesCategories(record.categories());
    }

    public boolean matchesCategories(List<String> paperCategories) {
        if (paperCategories == null || paperCategories.isEmpty()) {
            return false;
        }

        Set<String> paperCategorySet = new HashSet<>(paperCategories);
        return includedCategories.stream().anyMatch(paperCategorySet::contains);
    }
}
