package com.example.arxivrag.arxiv;

import com.example.arxivrag.paper.Paper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PaperMapper {

    private final ArxivRecordFilter filter;

    public PaperMapper(ArxivRecordFilter filter) {
        this.filter = filter;
    }

    public Paper map(ArxivRecord record) {
        if (record == null) {
            return null;
        }

        String arxivId = record.id().trim();
        String title = normalizeWhitespace(record.title());
        String abstractText = normalizeWhitespace(record.abstractText());
        
        List<String> authors = parseAuthors(record.authors());
        List<String> categories = parseCategories(record.categories());
        
        LocalDate submittedDate = filter.parseSubmittedDate(record);
        if (submittedDate == null) {
            submittedDate = LocalDate.now(); 
        }

        String doi = (record.doi() != null && !record.doi().trim().isEmpty()) ? record.doi().trim() : null;
        String journalRef = (record.journalRef() != null && !record.journalRef().trim().isEmpty()) ? record.journalRef().trim() : null;

        return new Paper(arxivId, title, abstractText, authors, categories, submittedDate, doi, journalRef);
    }

    public String normalizeWhitespace(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\s+", " ").trim();
    }

    private List<String> parseAuthors(String authorsStr) {
        if (authorsStr == null || authorsStr.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(authorsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private List<String> parseCategories(String categoriesStr) {
        if (categoriesStr == null || categoriesStr.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(categoriesStr.trim().split("\\s+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
