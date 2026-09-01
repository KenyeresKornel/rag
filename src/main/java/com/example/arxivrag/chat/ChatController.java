package com.example.arxivrag.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing standard and autonomous agent-based conversational RAG loops.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final RagChatService ragChatService;

    @Autowired
    public ChatController(RagChatService ragChatService) {
        this.ragChatService = ragChatService;
    }

    /**
     * Handles standard conversational RAG questions, retrieving context and generating grounded responses.
     */
    @PostMapping
    public RagChatResponse chat(@RequestBody RagChatRequest request) {
        return ragChatService.chat(request);
    }

    /**
     * Handles agent-based conversational questions with autonomous tool-calling capabilities.
     */
    @PostMapping("/agent")
    public RagChatResponse agentChat(@RequestBody RagChatRequest request) {
        return ragChatService.agentChat(request);
    }
}
