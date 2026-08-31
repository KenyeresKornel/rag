package com.example.arxivrag.embedding;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring Configuration conditionally registering our offline Mock {@link SimpleEmbeddingModel} as the Primary bean
 * when mock embeddings are enabled, allowing developers to run the entire pipeline without a paid OpenAI API key.
 */
@Configuration
public class MockEmbeddingConfig {

    @Bean
    @Primary
    @ConditionalOnProperty(name = "rag.embeddings.mock", havingValue = "true", matchIfMissing = true)
    public EmbeddingModel mockEmbeddingModel() {
        // Return 1536-dimensional mock embedding model matching text-embedding-3-small specifications
        return new SimpleEmbeddingModel(1536);
    }
}
