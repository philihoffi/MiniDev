package org.philipp.fun.minidev.core.phase.reviewing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.philipp.fun.minidev.core.phase.PhaseHandler;
import org.philipp.fun.minidev.core.GameStorageService;
import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.web.service.IdeSseService;
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

    private final IdeSseService ideSseService;
    private final GameStorageService gameStorageService;

    public ReviewingPhaseHandler(
            NotificationSseService notificationSseService,
            TerminalSseService terminalSseService,
            IdeSseService ideSseService,
            GameStorageService gameStorageService,
            LlmClient llmClient,
            ObjectMapper objectMapper) {
        this.notificationSseService = notificationSseService;
        this.terminalSseService = terminalSseService;
        this.ideSseService = ideSseService;
        this.gameStorageService = gameStorageService;
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
        terminalSseService.sendTerminalText("Performing quality assurance and To-Do assessment...\n", SseEventType.AGENT_WORK, 50);

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
                        "newTodos", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string")
                        ),
                        "consolidatedTodos", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string"),
                                "description", "The final, consolidated list of all REMAINING tasks (original open tasks + failed DONE tasks + new polish tasks), summarized for efficiency."
                        ),
                        "reviewSummary", Map.of("type", "string")
                ),
                "required", List.of("failedDoneTodos", "newTodos", "consolidatedTodos", "reviewSummary"),
                "additionalProperties", false
        );

        long fixingIterations = metadata.phaseHistory().stream()
                .filter(s -> s == RunState.FIXING)
                .count();

        boolean strictReview = fixingIterations >= 2;
        String reviewInstruction = strictReview ?
                "DO NOT add any more new tasks. ONLY identify critical bugs that prevent the game from being played. If it's playable, leave it as is." :
                "Review the code for general requirements: Does the game have clear instructions? Is the UI intuitive? Focus only on polish, usability, and clarity.";

        LlmRequest request = new LlmRequest(
                List.of(
                        LlmRequest.Message.system(String.format("""
                                You are a professional code reviewer. You are reviewing the progress of a game called '%s'.
                                The game concept is: %s
                                Core Mechanic: %s
                                
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
                                3. %s
                                4. If any such requirements are missing, formulate them as NEW tasks.
                                5. CONSOLIDATE ALL REMAINING TASKS:
                                    - Create a final, summarized list of all tasks that still need to be done.
                                    - This includes: original 'Open Tasks', 'failedDoneTodos' (tasks moved back from DONE), and 'newTodos'.
                                    - Combine redundant or overlapping tasks into single, clear, high-impact items.
                                    - Ensure the list is actionable and concise.
                                6. Respond with a JSON object:
                                    - 'failedDoneTodos': tasks from the DONE list that should be moved back to TODO.
                                    - 'newTodos': brand-new tasks identified in step 3.
                                    - 'consolidatedTodos': the final, summarized list of all remaining tasks (Steps 1, 2, 3, and 5 combined).
                                    - 'reviewSummary': explaining your reasoning.
                                """, metadata.name(), metadata.concept(), metadata.coreMechanic(), doneTodosFormatted, openTodosFormatted, code, reviewInstruction)),
                        LlmRequest.Message.user("Please provide the review results as JSON.")
                ), schema
        );

        LlmResponse response = llmClient.chat(request);

        if (response.success()) {
            try {
                String content = cleanJsonResponse(response.content());
                ReviewResponse reviewResponse = objectMapper.readValue(content, ReviewResponse.class);
                List<String> failedTodos = reviewResponse.failedDoneTodos();
                List<String> newTodos = reviewResponse.newTodos();

                int movedCount = 0;
                for (String failedTodo : failedTodos) {
                    if (metadata.doneTodos().remove(failedTodo)) {
                        metadata.todos().addFirst(failedTodo);
                        movedCount++;
                    }
                }

                int addedCount = 0;
                if (newTodos != null) {
                    for (String newTodo : newTodos) {
                        if (!metadata.todos().contains(newTodo) && !metadata.doneTodos().contains(newTodo)) {
                            metadata.todos().add(newTodo);
                            addedCount++;
                        }
                    }
                }

                // Step 3: Replace current todos with the consolidated/summarized list from LLM
                if (reviewResponse.consolidatedTodos() != null && !reviewResponse.consolidatedTodos().isEmpty()) {
                    metadata.todos().clear();
                    metadata.todos().addAll(reviewResponse.consolidatedTodos());
                    log.info("Consolidated To-Do list for run {} has {} items.", metadata.runId(), metadata.todos().size());
                }

                log.info("Successfully updated To-Dos for run {}. Moved {} tasks back to TODO, added {} new tasks. Summary: {}",
                        metadata.runId(), movedCount, addedCount, reviewResponse.reviewSummary());
                terminalSseService.sendTerminalText("Assessment completed: Updated " + movedCount + " existing and " + addedCount + " new backlog items.\n", SseEventType.AGENT_WORK, 50);

                // Ensure IDE is in sync
                Map<String, String> components = gameStorageService.getGameComponentContent(run.getGameMetadata().runId());
                if (components != null) {
                    // Just send full update to ensure everything is correct after review
                    ideSseService.sendFileUpdate("html", components.get("html"));
                    ideSseService.sendFileUpdate("css", components.get("css"));
                    ideSseService.sendFileUpdate("js", components.get("js"));
                }
            } catch (Exception e) {
                log.error("Failed to parse review response for run {}: {}", metadata.runId(), e.getMessage());
                notificationSseService.sendNotification("Review failed: " + e.getMessage());
            }
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
