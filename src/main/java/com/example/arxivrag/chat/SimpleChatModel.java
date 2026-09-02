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
 * Parses incoming prompts, detects if autonomous tool function options are enabled, simulates
 * agentic decisions (semanticSearch vs. hybridSearch), and returns realistic bracketed mock answers.
 */
public class SimpleChatModel implements ChatModel {

    @Override
    public ChatResponse call(Prompt prompt) {
        if (prompt == null) {
            return new ChatResponse(Collections.emptyList());
        }

        // Combine all message contents to parse the system prompt and instructions
        StringBuilder promptBuilder = new StringBuilder();
        prompt.getInstructions().forEach(msg -> promptBuilder.append(msg.getText()).append("\n"));
        String promptText = promptBuilder.toString();

        // Identify the actual user query (the last message in the list) for precise keyword scanning
        String userQuery = "";
        if (!prompt.getInstructions().isEmpty()) {
            userQuery = prompt.getInstructions().get(prompt.getInstructions().size() - 1).getText();
        }

        // 1. Detect if the prompt expects Agentic mode (by checking for standard tool names in the system prompt)
        boolean agentMode = promptText.contains("semanticSearch") || promptText.contains("hybridSearch");

        // 2. Scan for metadata tags inside the text context (if injected)
        Pattern docPattern = Pattern.compile("Document\\s*\\[(\\d+)\\]", Pattern.CASE_INSENSITIVE);
        Matcher docMatcher = docPattern.matcher(promptText);
        List<String> citedIndexes = new ArrayList<>();
        while (docMatcher.find()) {
            String index = docMatcher.group(1);
            if (!citedIndexes.contains(index)) {
                citedIndexes.add(index);
            }
        }

        // If in agent mode, we must simulate our tool call execution
        StringBuilder responseText = new StringBuilder();
        if (agentMode) {
            responseText.append("🔧 **[Agent Thought Trace]**\n");
            
            // Check if the actual user query contains a category or a year to simulate hybrid vs semantic routing
            boolean hasCategoryFilter = userQuery.contains("category") || userQuery.contains("cs.") || userQuery.contains("domain");
            boolean hasYearFilter = userQuery.contains("year") || userQuery.contains("published") || userQuery.contains("recent");

            if (hasCategoryFilter || hasYearFilter) {
                responseText.append("- User specified domain/date constraints. Decided to call tool **`hybridSearch`** with criteria:\n");
                if (hasCategoryFilter) responseText.append("  - `category`: \"cs.CL\"\n");
                if (hasYearFilter) responseText.append("  - `year`: 2024\n");
                responseText.append("- Tool `hybridSearch` returned 2 documents.\n\n");
            } else {
                responseText.append("- Query is generic and conceptual. Decided to call tool **`semanticSearch`**.\n");
                responseText.append("- Tool `semanticSearch` returned 2 documents.\n\n");
            }

            responseText.append("According to the autonomous search database results, several findings emerge:\n\n")
                .append("- **Core Findings**: High-capacity neural models exhibit robust transfer-learning capabilities [1].\n")
                .append("- **Optimizations**: Specific alignment methods reduce semantic shift [2].\n\n")
                .append("These aspects are detailed in the retrieved contexts, representing the grounded consensus.");
        } else {
            // Standard conversational RAG mode
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
