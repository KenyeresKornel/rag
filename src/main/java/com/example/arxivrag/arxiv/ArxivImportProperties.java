package com.example.arxivrag.arxiv;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.LocalDate;
import java.util.List;

@ConfigurationProperties(prefix = "arxiv.import")
public class ArxivImportProperties {

    private List<String> categories = List.of("cs.CL", "cs.AI", "cs.LG", "cs.CV");
    private LocalDate fromDate = LocalDate.of(2022, 1, 1);
    private Integer maxRecords = 50000;

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public Integer getMaxRecords() {
        return maxRecords;
    }

    public void setMaxRecords(Integer maxRecords) {
        this.maxRecords = maxRecords;
    }
}
