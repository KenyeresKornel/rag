package com.example.arxivrag.arxiv;

import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Component
public class ArxivDatasetReader {

    private static final Logger log = LoggerFactory.getLogger(ArxivDatasetReader.class);
    private final ObjectMapper objectMapper;

    public ArxivDatasetReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Stream<ArxivRecord> readRecords(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
        
        Iterator<ArxivRecord> iterator = new Iterator<>() {
            private String nextLine = null;
            private boolean finished = false;

            @Override
            public boolean hasNext() {
                if (finished) {
                    return false;
                }
                if (nextLine != null) {
                    return true;
                }
                try {
                    while (true) {
                        nextLine = reader.readLine();
                        if (nextLine == null) {
                            closeReader();
                            finished = true;
                            return false;
                        }
                        if (nextLine.trim().isEmpty()) {
                            continue;
                        }
                        return true;
                    }
                } catch (IOException e) {
                    log.error("Error reading next line from arXiv dataset", e);
                    closeReader();
                    finished = true;
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public ArxivRecord next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                while (true) {
                    String line = nextLine;
                    nextLine = null;
                    try {
                        return objectMapper.readValue(line, ArxivRecord.class);
                    } catch (Exception e) {
                        log.warn("Skipping malformed JSON line: {}", line, e);
                        if (!hasNext()) {
                            throw new NoSuchElementException("No more valid records in the file");
                        }
                    }
                }
            }

            private void closeReader() {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.warn("Error closing arXiv dataset BufferedReader", e);
                }
            }
        };

        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false)
                .onClose(() -> {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        log.warn("Error closing arXiv dataset stream reader", e);
                    }
                });
    }
}
