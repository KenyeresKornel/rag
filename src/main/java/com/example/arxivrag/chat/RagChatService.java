package com.example.arxivrag.chat;

import com.example.arxivrag.paper.Paper;
import com.example.arxivrag.paper.PaperRepository;
import com.example.arxivrag.vector.VectorSearchRequest;
import com.example.arxivrag.vector.VectorSearchResult;
import com.example.arxivrag.vector.VectorStoreGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service orchestrating the complete RAG loop: retrieve -> prompt ground -> generate -> cite.
 */
@Service
public class RagChatService {

    private static final Logger log = LoggerFactory.getLogger(RagChatService.class);

    private static final String SYSTEM_PROMPT_TEMPLATE = 
        "You are a helpful, professional scientific assistant answering questions based solely on the provided academic paper abstracts.\n\n" +
        "Constraints:\n" +
        "1. Base your answer STRICTLY on the retrieved context documents below. Do not use outside knowledge or hallucinate facts.\n" +
        "2. If the context does not contain enough information to answer the user's question, state clearly that you cannot answer based on the provided papers.\n" +
        "3. You MUST cite your sources using numeric brackets corresponding to the document index, e.g. [1] or [2] inside the sentences where you refer to that paper. Never list citations at the end, embed them directly in-line.\n\n" +
        "Context Documents:\n" +
        "{context}\n\n" +
        "User Question: {question}\n\n" +
        "Answer:";

    private final VectorStoreGateway vectorStoreGateway;
    private final PaperRepository paperRepository;
    private final ChatModel chatModel;

    @Autowired
    public RagChatService(
            VectorStoreGateway vectorStoreGateway,
            PaperRepository paperRepository,
            ChatModel chatModel) {
        this.vectorStoreGateway = vectorStoreGateway;
        this.paperRepository = paperRepository;
        this.chatModel = chatModel;
    }

    /**
     * Executes a complete conversational RAG query.
     */
    public RagChatResponse chat(RagChatRequest request) {
        String query = request.message();
        int topK = request.topK();
        
        log.info("Processing conversational RAG query: '{}' with topK: {}", query, topK);

        // 1. Retrieve semantically relevant documents from pgvector
        List<VectorSearchResult> searchResults = vectorStoreGateway.search(new VectorSearchRequest(query, topK));
        
        if (searchResults.isEmpty()) {
            log.info("No matching vectors found in the vector store.");
            return new RagChatResponse(
                "No semantically relevant papers were found in the database. Please ingest some papers first.",
                List.of()
            );
        }

        // 2. Resolve matching paper metadata back from PostgreSQL
        List<String> paperIds = searchResults.stream().map(VectorSearchResult::paperId).toList();
        List<Paper> dbPapers = paperRepository.findAllByArxivIdIn(paperIds);

        // Map PostgreSQL resolved papers back into the exact sort order of their vector similarity score
        Map<String, Paper> paperMap = dbPapers.stream()
            .collect(Collectors.toMap(Paper::getArxivId, Function.identity()));
        
        List<Paper> orderedPapers = new ArrayList<>();
        for (VectorSearchResult res : searchResults) {
            Paper p = paperMap.get(res.paperId());
            if (p != null) {
                orderedPapers.add(p);
            }
        }

        if (orderedPapers.isEmpty()) {
            log.warn("Matching vectors were retrieved, but no corresponding papers existed in PostgreSQL canonical table.");
            return new RagChatResponse(
                "Relevant document indexes were identified, but metadata details could not be resolved from PostgreSQL.",
                List.of()
            );
        }

        // 3. Construct the grounded system prompt containing enumerated context papers
        StringBuilder contextBuilder = new StringBuilder();
        for (int i = 0; i < orderedPapers.size(); i++) {
            Paper paper = orderedPapers.get(i);
            int index = i + 1; // 1-based indexing for LLM references
            contextBuilder.append("Document [").append(index).append("]:\n")
                .append("Title: ").append(paper.getTitle()).append("\n")
                .append("Abstract: ").append(paper.getAbstractText()).append("\n")
                .append("arXiv ID: ").append(paper.getArxivId()).append("\n\n");
        }

        PromptTemplate template = new PromptTemplate(SYSTEM_PROMPT_TEMPLATE);
        Prompt prompt = template.create(Map.of(
            "context", contextBuilder.toString(),
            "question", query
        ));

        // 4. Generate the response via ChatModel
        log.info("Invoking ChatModel to generate grounded response...");
        org.springframework.ai.chat.model.ChatResponse chatModelResponse = chatModel.call(prompt);
        String rawResponseText = chatModelResponse.getResult().getOutput().getText();

        // 5. Intercept numeric brackets (e.g., [1], [2]) to compile structured citations
        Pattern pattern = Pattern.compile("\\[(\\d+)\\]");
        Matcher matcher = pattern.matcher(rawResponseText);
        Set<Integer> citedIndexes = new HashSet<>();
        while (matcher.find()) {
            try {
                int citedIndex = Integer.parseInt(matcher.group(1));
                citedIndexes.add(citedIndex);
            } catch (NumberFormatException e) {
                // Ignore malformed digits
            }
        }

        List<Citation> citations = new ArrayList<>();
        for (int citedIndex : citedIndexes) {
            int listIndex = citedIndex - 1; // Translate back to 0-based list index
            if (listIndex >= 0 && listIndex < orderedPapers.size()) {
                Paper paper = orderedPapers.get(listIndex);
                citations.add(new Citation(paper.getArxivId(), paper.getTitle(), paper.getAuthors()));
            }
        }

        log.info("Grounded response generated with {} active citations.", citations.size());
        return new RagChatResponse(rawResponseText, citations);
    }
}
