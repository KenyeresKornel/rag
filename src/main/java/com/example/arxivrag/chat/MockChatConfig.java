package com.example.arxivrag.chat;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring Configuration conditionally registering our offline Mock {@link SimpleChatModel} as the Primary bean
 * when mock chat is enabled, allowing developers to run and test the conversational RAG loop locally.
 */
@Configuration
public class MockChatConfig {

    @Bean
    @Primary
    @ConditionalOnProperty(name = "rag.chat.mock", havingValue = "true", matchIfMissing = true)
    public ChatModel mockChatModel() {
        return new SimpleChatModel();
    }
}
