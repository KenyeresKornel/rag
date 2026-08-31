package com.example.arxivrag.embedding;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A high-performance, deterministic, offline mock implementation of Spring AI's {@link EmbeddingModel}.
 * Generates identical, normalized vector dimensions based on the MD5 hash of the input text, enabling
 * completely free and local offline development, testing, and CI runs.
 */
public class SimpleEmbeddingModel implements EmbeddingModel {

    private final int dimensions;

    public SimpleEmbeddingModel() {
        this(1536); // Default to 1536 dimensions matching text-embedding-3-small
    }

    public SimpleEmbeddingModel(int dimensions) {
        this.dimensions = dimensions;
    }

    @Override
    public int dimensions() {
        return this.dimensions;
    }

    @Override
    public float[] embed(Document document) {
        if (document == null) {
            return new float[this.dimensions];
        }
        return generateDeterministicVector(document.getFormattedContent());
    }

    @Override
    public float[] embed(String text) {
        return generateDeterministicVector(text);
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        if (texts == null) {
            return Collections.emptyList();
        }
        return texts.stream().map(this::generateDeterministicVector).toList();
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        if (request == null) {
            return new EmbeddingResponse(Collections.emptyList(), new EmbeddingResponseMetadata());
        }

        List<Embedding> list = new ArrayList<>();
        int index = 0;
        for (String instruction : request.getInstructions()) {
            float[] vector = generateDeterministicVector(instruction);
            list.add(new Embedding(vector, index++));
        }
        return new EmbeddingResponse(list, new EmbeddingResponseMetadata());
    }

    /**
     * Generates a deterministic float array of the specified dimension based on the MD5 hash of the text.
     */
    private float[] generateDeterministicVector(String text) {
        float[] vector = new float[this.dimensions];
        if (text == null || text.isEmpty()) {
            return vector;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));

            // Seed Java's Random generator with the first 8 bytes of the MD5 hash
            long seed = 0;
            for (int i = 0; i < Math.min(hash.length, 8); i++) {
                seed = (seed << 8) | (hash[i] & 0xFF);
            }

            Random random = new Random(seed);
            double sumOfSquares = 0;
            for (int i = 0; i < this.dimensions; i++) {
                float val = random.nextFloat() * 2 - 1; // value between -1.0 and 1.0
                vector[i] = val;
                sumOfSquares += val * val;
            }

            // Normalize the vector (L2 norm) to ensure cosine similarity calculation matches math standards
            float norm = (float) Math.sqrt(sumOfSquares);
            if (norm > 0) {
                for (int i = 0; i < this.dimensions; i++) {
                    vector[i] /= norm;
                }
            }
        } catch (NoSuchAlgorithmException e) {
            // Safe fallback hash
            for (int i = 0; i < this.dimensions; i++) {
                vector[i] = (float) (text.hashCode() + i) / (text.hashCode() + i + 1);
            }
        }
        return vector;
    }
}
