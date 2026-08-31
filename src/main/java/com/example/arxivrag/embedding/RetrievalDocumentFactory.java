package com.example.arxivrag.embedding;

import com.example.arxivrag.paper.Paper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Factory for building normalized, hashed RetrievalDocuments from canonical Paper entities.
 */
public final class RetrievalDocumentFactory {

    private RetrievalDocumentFactory() {
        // Prevent instantiation
    }

    /**
     * Normalizes the string by collapsing all sequences of whitespace (including newlines) into a single space.
     */
    public static String normalizeText(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("\\s+", " ").trim();
    }

    /**
     * Converts a Paper entity into a formatted, hashed RetrievalDocument.
     */
    public static RetrievalDocument createDocument(Paper paper) {
        String normalizedTitle = normalizeText(paper.getTitle());
        String normalizedAbstract = normalizeText(paper.getAbstractText());

        String formattedText = "Title: " + normalizedTitle + "\n\nAbstract:\n" + normalizedAbstract;
        String contentHash = calculateSha256(formattedText);
        String arxivId = paper.getArxivId();

        String documentId = "arxiv:" + arxivId + ":chunk:0";

        Map<String, Object> metadata = Map.of(
            "paper_id", arxivId,
            "chunk_id", "0",
            "categories", paper.getCategories(),
            "submitted_date", paper.getSubmittedDate().toString()
        );

        return new RetrievalDocument(documentId, arxivId, formattedText, metadata, contentHash);
    }

    /**
     * Computes the SHA-256 hash of a string, returning it as a 64-character hex string.
     */
    public static String calculateSha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 MessageDigest is not available", e);
        }
    }
}
