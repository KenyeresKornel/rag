package com.cgi.rag.arxiv;

import com.cgi.rag.config.ArxivImportProperties;
import org.springframework.stereotype.Component;

/**
 * Stateless policy for deciding whether the importer may accept another
 * matching record. The importer owns the accepted-record counter so a new
 * import run always starts from zero.
 */
@Component
public class ArxivImportSelectionPolicy {

    private final Integer maxRecords;

    public ArxivImportSelectionPolicy(ArxivImportProperties properties) {
        this.maxRecords = properties.maxRecords();
    }

    public boolean canAcceptAnother(int acceptedRecords) {
        if (acceptedRecords < 0) {
            throw new IllegalArgumentException("acceptedRecords must not be negative");
        }
        return maxRecords == null || acceptedRecords < maxRecords;
    }

    public boolean isUnlimited() {
        return maxRecords == null;
    }
}
