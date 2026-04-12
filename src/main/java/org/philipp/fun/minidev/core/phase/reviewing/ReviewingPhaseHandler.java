package org.philipp.fun.minidev.core.phase.reviewing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.philipp.fun.minidev.core.phase.PhaseHandler;
import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.llm.LlmRequest;
import org.philipp.fun.minidev.llm.LlmResponse;
import org.philipp.fun.minidev.run.AgentRun;
import org.philipp.fun.minidev.run.AgentRun.RunState;
import org.philipp.fun.minidev.run.GameMetadata;
import org.philipp.fun.minidev.web.service.NotificationSseService;
import org.philipp.fun.minidev.web.service.TerminalSseService;
import org.philipp.fun.minidev.web.service.AbstractSseService.SseEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ReviewingPhaseHandler implements PhaseHandler {

    private static final Logger log = LoggerFactory.getLogger(ReviewingPhaseHandler.class);

    private final NotificationSseService notificationSseService;
    private final TerminalSseService terminalSseService;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public ReviewingPhaseHandler(
            NotificationSseService notificationSseService,
            TerminalSseService terminalSseService,
            LlmClient llmClient,
            ObjectMapper objectMapper) {
        this.notificationSseService = notificationSseService;
        this.terminalSseService = terminalSseService;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void execute(AgentRun run) {
        GameMetadata metadata = run.getGameMetadata();
        if (metadata == null) {
            log.error("No metadata found for run {}", run.getGameMetadata().runId());
            run.transitionTo(RunState.FAILED);
            return;
        }

        log.info("Starting review phase for run {}", run.getGameMetadata().runId());
        terminalSseService.sendTerminalText("Reviewing code and updating To-Dos...\n", SseEventType.AGENT_WORK, 50);

        String code = "";
        try {
            if (Files.exists(metadata.htmlPath())) {
                code = Files.readString(metadata.htmlPath());
            }
        } catch (IOException e) {
            log.error("Failed to read code for review from {}", metadata.htmlPath(), e);
        }

        String doneTodosFormatted = String.join("\n", metadata.doneTodos().stream().map(t -> "- " + t).toList());
        String openTodosFormatted = String.join("\n", metadata.todos().stream().map(t -> "- " + t).toList());

        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "failedDoneTodos", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string")
                        ),
                        "reviewSummary", Map.of("type", "string")
                ),
                "required", List.of("failedDoneTodos", "reviewSummary"),
                "additionalProperties", false
        );

        LlmRequest request = new LlmRequest(
                List.of(
                        LlmRequest.Message.system(String.format("""
                                You are a professional code reviewer. You are reviewing the progress of a game called '%s'.
                                The game concept is: %s
                                
                                Tasks marked as completed (DONE):
                                %s
                                Open Tasks (STILL TO DO):
                                %s
                                
                                Current Implementation (HTML/JS/CSS):
                                --- START CODE ---
                                %s
                                --- END CODE ---
                                
                                Your Task:
                                1. Evaluate whether the tasks in the 'DONE' list are actually fully and correctly implemented in the provided code.
                                2. If a task from the 'DONE' list is NOT or only partially implemented, identify it.
                                3. DO NOT add any brand-new tasks. Only evaluate the existing 'DONE' list.
                                4. Respond with a JSON object containing a list of 'failedDoneTodos' (tasks from the DONE list that should be moved back to TODO) and a 'reviewSummary' explaining your reasoning.
                                """, metadata.name(), metadata.concept(), doneTodosFormatted, openTodosFormatted, code)),
                        LlmRequest.Message.user("Please provide the review results as JSON.")
                ), schema
        );

        LlmResponse response = llmClient.chat(request);

        if (response.success()) {
            try {
                String content = cleanJsonResponse(response.content());
                ReviewResponse reviewResponse = objectMapper.readValue(content, ReviewResponse.class);
                List<String> failedTodos = reviewResponse.failedDoneTodos();

                int movedCount = 0;
                for (String failedTodo : failedTodos) {
                    if (metadata.doneTodos().remove(failedTodo)) {
                        metadata.todos().addFirst(failedTodo);
                        movedCount++;
                    }
                }


                log.info("Successfully updated To-Dos for run {}. Moved {} tasks back to TODO. Summary: {}",
                        metadata.runId(), movedCount, reviewResponse.reviewSummary());
                terminalSseService.sendTerminalText("Review completed. Moved " + movedCount + " tasks back to TODO list.\n", SseEventType.AGENT_WORK, 50);
            } catch (Exception e) {
                log.error("Failed to parse review response for run {}: {}", metadata.runId(), e.getMessage());
                notificationSseService.sendNotification("Review failed: " + e.getMessage());
            }
            metadata.doneTodos().clear();
        } else {
            log.error("LLM review failed for run {}: {}", metadata.runId(), response.errorMessage());
            notificationSseService.sendNotification("Review failed: " + response.errorMessage());
        }
    }

    private String cleanJsonResponse(String content) {
        content = content.trim();
        if (content.startsWith("```json")) {
            content = content.substring(7);
        } else if (content.startsWith("```")) {
            content = content.substring(3);
        }
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }
        return content.trim();
    }
}
