package com.example.arxivrag.arxiv;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Component
public class ArxivRecordFilter {

    private final ArxivImportProperties properties;

    public ArxivRecordFilter(ArxivImportProperties properties) {
        this.properties = properties;
    }

    public boolean test(ArxivRecord record) {
        if (record == null) {
            return false;
        }

        // 1. Filter by categories
        if (record.categories() == null || record.categories().trim().isEmpty()) {
            return false;
        }
        
        List<String> recordCategories = Arrays.asList(record.categories().trim().split("\\s+"));
        List<String> configuredCategories = properties.getCategories();
        
        boolean categoryMatched = recordCategories.stream()
                .anyMatch(configuredCategories::contains);
                
        if (!categoryMatched) {
            return false;
        }

        // 2. Filter by date range
        LocalDate submittedDate = parseSubmittedDate(record);
        if (submittedDate == null) {
            return false;
        }

        LocalDate fromDate = properties.getFromDate();
        if (fromDate != null && submittedDate.isBefore(fromDate)) {
            return false;
        }

        return true;
    }

    public LocalDate parseSubmittedDate(ArxivRecord record) {
        if (record.versions() != null && !record.versions().isEmpty()) {
            for (ArxivRecord.Version v : record.versions()) {
                if ("v1".equalsIgnoreCase(v.version())) {
                    LocalDate date = parseArxivDateStr(v.created());
                    if (date != null) {
                        return date;
                    }
                }
            }
            LocalDate date = parseArxivDateStr(record.versions().get(0).created());
            if (date != null) {
                return date;
            }
        }

        if (record.updateDate() != null) {
            try {
                return LocalDate.parse(record.updateDate().trim());
            } catch (Exception e) {
                // Ignore
            }
        }

        return null;
    }

    private LocalDate parseArxivDateStr(String dateStr) {
        if (dateStr == null) {
            return null;
        }
        try {
            String[] parts = dateStr.trim().replaceAll("\\s+", " ").split(" ");
            if (parts.length >= 4) {
                String dayOfWeek = parts[0]; 
                String day = parts[1]; 
                String month = parts[2]; 
                String year = parts[3]; 
                String dateToParse = dayOfWeek + " " + day + " " + month + " " + year;
                
                java.time.format.DateTimeFormatter parser = java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM yyyy", java.util.Locale.US);
                return LocalDate.parse(dateToParse, parser);
            }
        } catch (Exception e) {
            // Skip
        }
        return null;
    }
}
