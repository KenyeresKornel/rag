package com.example.arxivrag.chat;

/**
 * DTO representing a user's conversational chat request.
 */
public record RagChatRequest(
    String message,
    Integer topK
) {
    public RagChatRequest {
        if (topK == null) {
            topK = 5; // Default to retrieving top-5 papers as grounded context
        }
    }
}
