package com.example.arxivrag.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * CommandLineRunner executing the full retriever-generator conversational loop when called with 'chat-rag'.
 */
@Component
public class ChatCommandLineRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ChatCommandLineRunner.class);

    private final RagChatService ragChatService;
    private final ApplicationContext applicationContext;

    @Autowired
    public ChatCommandLineRunner(RagChatService ragChatService, ApplicationContext applicationContext) {
        this.ragChatService = ragChatService;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length > 0 && "chat-rag".equals(args[0])) {
            if (args.length < 2) {
                System.out.println("Error: Missing question string. Usage: chat-rag \"<question>\"");
                int exitCode = SpringApplication.exit(applicationContext, () -> 1);
                System.exit(exitCode);
            }

            // Group all subsequent arguments into a single search question
            String question = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
            System.out.println("\n>>> Querying Conversational RAG loop for: \"" + question + "\"\n");

            try {
                RagChatResponse chatResponse = ragChatService.chat(new RagChatRequest(question, 5));

                System.out.println("==========================================================================================");
                System.out.println("GROUNDED ANSWER:");
                System.out.println("==========================================================================================");
                System.out.println(chatResponse.responseText());
                System.out.println("==========================================================================================");
                
                if (chatResponse.citations().isEmpty()) {
                    System.out.println("CITATIONS: None.");
                } else {
                    System.out.println("RESOLVED CITATIONS:");
                    System.out.println("------------------------------------------------------------------------------------------");
                    for (int i = 0; i < chatResponse.citations().size(); i++) {
                        Citation citation = chatResponse.citations().get(i);
                        System.out.printf("[%d] %s%n", i + 1, citation.title());
                        System.out.printf("    Authors: %s%n", String.join(", ", citation.authors()));
                        System.out.printf("    arXiv Link: %s%n", citation.url());
                        System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - ");
                    }
                }
                System.out.println("==========================================================================================\n");

                int exitCode = SpringApplication.exit(applicationContext, () -> 0);
                System.exit(exitCode);
            } catch (Exception e) {
                log.error("Fatal error during CLI conversational RAG query", e);
                int exitCode = SpringApplication.exit(applicationContext, () -> 1);
                System.exit(exitCode);
            }
        }
    }
}
