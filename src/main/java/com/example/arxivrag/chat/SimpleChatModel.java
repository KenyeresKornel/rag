package com.example.arxivrag.chat;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A highly sophisticated, local offline mock of Spring AI's {@link ChatModel}.
 * Parses incoming prompt contexts for document markers (like Document [1]: ...) and dynamically
 * structures a deterministic response that cites them using bracket notation, allowing full RAG loop validation.
 */
public class SimpleChatModel implements ChatModel {

    @Override
    public ChatResponse call(Prompt prompt) {
        if (prompt == null) {
            return new ChatResponse(Collections.emptyList());
        }

        // Combine all message contents to parse the system prompt
        StringBuilder promptBuilder = new StringBuilder();
        prompt.getInstructions().forEach(msg -> promptBuilder.append(msg.getText()).append("\n"));
        String promptText = promptBuilder.toString();

        // Regex scan for any "Document [x]:" markers to see what was injected as context
        Pattern pattern = Pattern.compile("Document\\s*\\[(\\d+)\\]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(promptText);
        List<String> citedIndexes = new ArrayList<>();
        while (matcher.find()) {
            String index = matcher.group(1);
            if (!citedIndexes.contains(index)) {
                citedIndexes.add(index);
            }
        }

        // Build a highly realistic, grounded markdown answer citing these documents
        StringBuilder responseText = new StringBuilder();
        responseText.append("According to the provided research papers, there are multiple approaches to your inquiry:\n\n");

        if (citedIndexes.isEmpty()) {
            responseText.append("No context documents were provided in the prompt, so I am answering with general domain knowledge. Please ingest and index relevant papers to ground this response.");
        } else {
            responseText.append("- **Core Methodology**: Recent developments demonstrate significant performance benefits using robust architectures ");
            responseText.append("[").append(citedIndexes.get(0)).append("].\n");
            
            if (citedIndexes.size() > 1) {
                responseText.append("- **Alternative Frameworks**: Other researched methodologies present complementary performance metrics ");
                responseText.append("[").append(citedIndexes.get(1)).append("].\n\n");
            } else {
                responseText.append("\n");
            }
            
            responseText.append("These observations represent the primary consensus from the analyzed abstracts, illustrating key trade-offs in efficiency and accuracy.");
        }

        AssistantMessage assistantMessage = new AssistantMessage(responseText.toString());
        Generation generation = new Generation(assistantMessage);
        
        return new ChatResponse(List.of(generation));
    }

    @Override
    public ChatOptions getOptions() {
        return ChatOptions.builder().build();
    }
}
