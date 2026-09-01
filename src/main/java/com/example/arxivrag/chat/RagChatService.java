package com.example.arxivrag.chat;

import com.example.arxivrag.paper.Paper;
import com.example.arxivrag.paper.PaperRepository;
import com.example.arxivrag.vector.VectorSearchRequest;
import com.example.arxivrag.vector.VectorSearchResult;
import com.example.arxivrag.vector.VectorStoreGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
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
 * Service orchestrating both standard RAG retrieval and autonomous agentic tool-routing pipelines.
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

    private static final String AGENT_SYSTEM_PROMPT = 
        "You are an autonomous scientific agent helping researchers explore arXiv papers.\n\n" +
        "You have access to highly powerful database search tools: 'semanticSearch' and 'hybridSearch'.\n\n" +
        "Constraints:\n" +
        "1. Base your final answer ONLY on the paper contexts returned by the tools. If a tool reports no papers were found, state that clearly.\n" +
        "2. You MUST cite your sources in-line using numeric brackets corresponding to the Document index returned by the tool (e.g., [1], [2]).\n" +
        "3. Be proactive: if the user specifies a particular category (like cs.CV, cs.AI, cs.CL, cs.LG) or a year, use 'hybridSearch' to query with structured constraints. If they ask general concepts, use 'semanticSearch'.";

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
     * Executes a complete standard conversational RAG query.
     */
    public RagChatResponse chat(RagChatRequest request) {
        String query = request.message();
        int topK = request.topK();
        
        log.info("Processing standard RAG query: '{}' with topK: {}", query, topK);

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

        log.info("Standard response generated with {} active citations.", citations.size());
        return new RagChatResponse(rawResponseText, citations);
    }

    /**
     * Executes an autonomous, agent-based query with tool-calling capabilities.
     */
    public RagChatResponse agentChat(RagChatRequest request) {
        String query = request.message();
        int topK = request.topK();
        log.info("Processing Agentic RAG query: '{}' with topK: {}", query, topK);

        SystemMessage systemMessage = new SystemMessage(AGENT_SYSTEM_PROMPT);
        UserMessage userMessage = new UserMessage(query);

        // Prompt invokes the ChatModel. Function options are bound globally via application.yml config
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        log.info("Invoking ChatModel with autonomous search functions enabled...");
        org.springframework.ai.chat.model.ChatResponse modelResponse = chatModel.call(prompt);
        String rawResponseText = modelResponse.getResult().getOutput().getText();

        // Parse Citations. Since the agent executes the search internally, we retrieve candidates
        // matches from pgvector based on the original query, and verify if they are cited/mentioned in the LLM text.
        List<VectorSearchResult> candidates = vectorStoreGateway.search(new VectorSearchRequest(query, topK + 2)); // scan slightly wider
        List<Citation> citations = new ArrayList<>();

        if (!candidates.isEmpty()) {
            List<String> paperIds = candidates.stream().map(VectorSearchResult::paperId).toList();
            List<Paper> dbPapers = paperRepository.findAllByArxivIdIn(paperIds);

            for (Paper paper : dbPapers) {
                // If the LLM mentions the paper's unique arXiv ID or standard title keywords in its answer
                if (rawResponseText.contains(paper.getArxivId()) || 
                    (paper.getTitle().length() > 15 && rawResponseText.contains(paper.getTitle().substring(0, 15)))) {
                    citations.add(new Citation(paper.getArxivId(), paper.getTitle(), paper.getAuthors()));
                }
            }
        }

        // Fallback: If no papers matched candidate strings but text still has generic [1] brackets
        if (citations.isEmpty()) {
            Pattern pattern = Pattern.compile("\\[(\\d+)\\]");
            Matcher matcher = pattern.matcher(rawResponseText);
            Set<Integer> citedIndexes = new HashSet<>();
            while (matcher.find()) {
                try {
                    citedIndexes.add(Integer.parseInt(matcher.group(1)));
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
            if (!citedIndexes.isEmpty() && !candidates.isEmpty()) {
                List<String> paperIds = candidates.stream().map(VectorSearchResult::paperId).toList();
                List<Paper> dbPapers = paperRepository.findAllByArxivIdIn(paperIds);
                for (int idx : citedIndexes) {
                    int listIndex = idx - 1;
                    if (listIndex >= 0 && listIndex < dbPapers.size()) {
                        Paper paper = dbPapers.get(listIndex);
                        citations.add(new Citation(paper.getArxivId(), paper.getTitle(), paper.getAuthors()));
                    }
                }
            }
        }

        log.info("Agentic response generated with {} active citations.", citations.size());
        return new RagChatResponse(rawResponseText, citations);
    }
}
