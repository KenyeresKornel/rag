package com.example.arxivrag.arxiv;

import com.example.arxivrag.paper.Paper;
import com.example.arxivrag.paper.PaperRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ArxivImportService {

    private static final Logger log = LoggerFactory.getLogger(ArxivImportService.class);

    private final ArxivDatasetReader reader;
    private final ArxivRecordFilter filter;
    private final PaperMapper mapper;
    private final PaperRepository paperRepository;
    private final ArxivImportProperties properties;

    public ArxivImportService(ArxivDatasetReader reader, ArxivRecordFilter filter, PaperMapper mapper,
                              PaperRepository paperRepository, ArxivImportProperties properties) {
        this.reader = reader;
        this.filter = filter;
        this.mapper = mapper;
        this.paperRepository = paperRepository;
        this.properties = properties;
    }

    public ImportStats importDataset(File file) throws IOException {
        log.info("Starting arXiv dataset import from file: {}", file.getAbsolutePath());
        ImportStats stats = new ImportStats();
        
        List<ArxivRecord> batch = new ArrayList<>();
        int batchSize = 1000;
        Integer limit = properties.getMaxRecords();

        try (Stream<ArxivRecord> recordStream = reader.readRecords(file)) {
            var iterator = recordStream.iterator();
            while (iterator.hasNext()) {
                ArxivRecord record = iterator.next();
                stats.incrementRead();

                if (filter.test(record)) {
                    stats.incrementMatched();
                    batch.add(record);
                    if (batch.size() >= batchSize) {
                        saveBatch(batch, stats);
                        batch.clear();
                    }
                } else {
                    stats.incrementSkipped();
                }

                if (limit != null && (stats.getImported() + stats.getUpdated()) >= limit) {
                    log.info("Reached maximum record limit of: {}", limit);
                    break;
                }
            }

            if (!batch.isEmpty() && (limit == null || (stats.getImported() + stats.getUpdated()) < limit)) {
                saveBatch(batch, stats);
            }
        }

        String summary = stats.getFormattedSummary();
        System.out.println(summary);
        log.info("arXiv import completed. Summary: \n{}", summary);
        return stats;
    }

    @Transactional
    public void saveBatch(List<ArxivRecord> recordBatch, ImportStats stats) {
        List<Paper> mappedPapers = recordBatch.stream()
                .map(mapper::map)
                .collect(Collectors.toList());

        List<String> arxivIds = mappedPapers.stream()
                .map(Paper::getArxivId)
                .collect(Collectors.toList());

        List<Paper> existing = paperRepository.findAllByArxivIdIn(arxivIds);
        Map<String, Paper> existingMap = existing.stream()
                .collect(Collectors.toMap(Paper::getArxivId, p -> p));

        List<Paper> toSave = new ArrayList<>();
        Integer limit = properties.getMaxRecords();

        for (Paper paper : mappedPapers) {
            if (limit != null && (stats.getImported() + stats.getUpdated()) >= limit) {
                break;
            }

            Paper existingPaper = existingMap.get(paper.getArxivId());
            if (existingPaper != null) {
                paper.setId(existingPaper.getId());
                paper.setCreatedAt(existingPaper.getCreatedAt());
                toSave.add(paper);
                stats.incrementUpdated();
            } else {
                toSave.add(paper);
                stats.incrementImported();
            }
        }

        if (!toSave.isEmpty()) {
            paperRepository.saveAll(toSave);
            paperRepository.flush();
        }
    }
}
