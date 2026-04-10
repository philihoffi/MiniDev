package org.philipp.fun.minidev.web;

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

    private final LlmClient llmClient;

    public ChatController(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @PostMapping("/chat")
    public LlmResponse chat(@RequestBody ChatApiRequest request) {
        if (request.message() == null || request.message().isBlank()) {
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
            return LlmResponse.failure("This API Endpoint is for demo purposes only. Messages cannot exceed 4096 Characters.");
        }

        LlmRequest llmRequest = new LlmRequest(messages);
        return llmClient.chat(llmRequest);
    }
}
