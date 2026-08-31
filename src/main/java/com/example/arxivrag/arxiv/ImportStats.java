package com.example.arxivrag.arxiv;

import java.time.Duration;
import java.time.Instant;

public class ImportStats {
    private long recordsRead = 0;
    private long categoryMatched = 0;
    private long imported = 0;
    private long updated = 0;
    private long skipped = 0;
    private long invalid = 0;
    private Instant startTime = Instant.now();

    public synchronized void incrementRead() { recordsRead++; }
    public synchronized void incrementMatched() { categoryMatched++; }
    public synchronized void incrementImported() { imported++; }
    public synchronized void incrementUpdated() { updated++; }
    public synchronized void incrementSkipped() { skipped++; }
    public synchronized void incrementInvalid() { invalid++; }

    public long getRecordsRead() { return recordsRead; }
    public long getCategoryMatched() { return categoryMatched; }
    public long getImported() { return imported; }
    public long getUpdated() { return updated; }
    public long getSkipped() { return skipped; }
    public long getInvalid() { return invalid; }

    public String getFormattedSummary() {
        Duration duration = Duration.between(startTime, Instant.now());
        long seconds = duration.toSeconds();
        long millis = duration.toMillis() % 1000;
        
        return String.format(
            "arXiv import complete\n\n" +
            "Records read:       %,15d\n" +
            "Category matched:   %,15d\n" +
            "Imported (new):     %,15d\n" +
            "Updated:            %,15d\n" +
            "Skipped:            %,15d\n" +
            "Invalid records:    %,15d\n" +
            "Elapsed:            %d.%03d s\n",
            recordsRead, categoryMatched, imported, updated, skipped, invalid, seconds, millis
        );
    }
}
