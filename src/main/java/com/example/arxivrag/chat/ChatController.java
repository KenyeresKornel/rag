package com.example.arxivrag.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing the conversational RAG chat loop.
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
     * Handles incoming grounded chat messages, retrieving context and generating conversational, cited responses.
     */
    @PostMapping
    public RagChatResponse chat(@RequestBody RagChatRequest request) {
        return ragChatService.chat(request);
    }
}
