package org.philipp.fun.minidev.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.llm.LlmRequest;
import org.philipp.fun.minidev.llm.LlmResponse;
import org.philipp.fun.minidev.web.objects.ChatApiRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final LlmClient llmClient;

    public ChatController(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @PostMapping("/chat")
    public LlmResponse chat(@RequestBody ChatApiRequest request) {
        log.info("Received chat request for message: {}", request.message());
        if (request.message() == null || request.message().isBlank()) {
            log.warn("Chat request failed: Message is empty");
            return LlmResponse.failure("Message is required");
        }

        List<LlmRequest.Message> messages = new ArrayList<>();

        if (request.history() != null) {
            for (ChatApiRequest.HistoryEntry entry : request.history()) {
                String role = entry.role();
                String content = entry.content();
                if ("user".equals(role)) {
                    messages.add(LlmRequest.Message.user(content));
                } else if ("assistant".equals(role)) {
                    messages.add(LlmRequest.Message.assistant(content));
                }
            }
        }

        messages.add(LlmRequest.Message.user(request.message()));

        //Protection :)
        int totalTokens = 0;
        for (LlmRequest.Message msg : messages) {
            totalTokens += msg.content().length();
        }
        if (totalTokens > 4096) {
            log.warn("Chat request blocked: Token limit exceeded ({} tokens)", totalTokens);
            return LlmResponse.failure("This API Endpoint is for demo purposes only. Messages cannot exceed 4096 Characters.");
        }

        LlmRequest llmRequest = new LlmRequest(messages);
        LlmResponse response = llmClient.chat(llmRequest);
        
        if (response.success()) {
            log.info("Chat response successful from model: {}", response.model());
        } else {
            log.error("Chat response failed: {}", response.errorMessage());
        }
        
        return response;
    }
}
