package com.example.arxivrag.chat;

import java.util.List;

/**
 * DTO representing the grounded, conversable response from the RAG system.
 */
public record RagChatResponse(
    String responseText,
    List<Citation> citations
) {}
