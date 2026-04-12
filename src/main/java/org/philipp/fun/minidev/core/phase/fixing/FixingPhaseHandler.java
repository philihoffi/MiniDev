package org.philipp.fun.minidev.core.phase.fixing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.philipp.fun.minidev.core.phase.PhaseHandler;
import org.philipp.fun.minidev.core.GameStorageService;
import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.llm.LlmRequest;
import org.philipp.fun.minidev.llm.LlmResponse;
import org.philipp.fun.minidev.run.AgentRun;
import org.philipp.fun.minidev.run.AgentRun.RunState;
import org.philipp.fun.minidev.run.GameMetadata;
import org.philipp.fun.minidev.web.service.IdeSseService;
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
public class FixingPhaseHandler implements PhaseHandler {

    private static final Logger log = LoggerFactory.getLogger(FixingPhaseHandler.class);

    private final NotificationSseService notificationSseService;
    private final TerminalSseService terminalSseService;
    private final IdeSseService ideSseService;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    private final GameStorageService gameStorageService;

    public FixingPhaseHandler(
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

        terminalSseService.sendTerminalText("Addressing all identified issues for " + metadata.name() + "\n Total pending items: " + metadata.todos().size() + "\n", SseEventType.AGENT_WORK, 50);

        if (!run.getGameMetadata().todos().isEmpty()) {
            metadata = run.getGameMetadata();
            List<String> currentTodos = new ArrayList<>(metadata.todos());
            String todosFormatted = String.join("\n", currentTodos.stream().map(t -> "- " + t).toList());
            
            log.info("Fixing all To-Dos for run {}: {}", metadata.runId(), currentTodos);
            terminalSseService.sendTerminalText("Processing all tasks:\n" + todosFormatted + "\n", SseEventType.AGENT_WORK, 50);

            String currentCode = "";
            try {
                if (Files.exists(metadata.htmlPath())) {
                    currentCode = Files.readString(metadata.htmlPath());
                }
            } catch (IOException e) {
                log.error("Failed to read code for fixing from {}", metadata.htmlPath(), e);
                return;
            }

            String doneTodosFormatted = String.join("\n", metadata.doneTodos().stream().map(t -> "- " + t).toList());

            Map<String, Object> schema = Map.of(
                    "type", "object",
                    "properties", Map.of(
                            "updatedCode", Map.of("type", "string"),
                            "changesSummary", Map.of("type", "string")
                    ),
                    "required", List.of("updatedCode", "changesSummary"),
                    "additionalProperties", false
            );

            LlmRequest request = new LlmRequest(
                    List.of(
                            LlmRequest.Message.system(String.format("""
                                    You are a professional web developer. Your task is to implement several features and fixes in a game called '%s'.
                                    The game concept is: %s
                                    Core Mechanic: %s
                                    
                                    Already completed tasks:
                                    %s
                                    
                                    YOUR CURRENT TASKS TO IMPLEMENT:
                                    %s
                                    
                                    Current Implementation (HTML/JS/CSS):
                                    --- START CODE ---
                                    %s
                                    --- END CODE ---
                                    
                                    Instructions:
                                    1. Review the current implementation and the completed tasks.
                                    2. Update the code to implement ALL 'CURRENT TASKS TO IMPLEMENT'.
                                    3. Fix any bugs or inconsistencies related to this task.
                                    4. Ensure the game remains a single HTML file with embedded CSS and JS.
                                    
                                    Respond with a JSON object containing the updated code and a short summary of the changes.
                                    """, metadata.name(), metadata.concept(), metadata.coreMechanic(), doneTodosFormatted, todosFormatted, currentCode)),
                            LlmRequest.Message.user("Please provide the updated code with all requested changes.")
                    ),
                    null,
                    null,
                    true,
                    schema,
                    metadata.runId().toString()
            );

            LlmResponse response = llmClient.chat(request);

            if (response.success()) {
                try {
                    String content = cleanJsonResponse(response.content());
                    FixingResponse fixingResponse = objectMapper.readValue(content, FixingResponse.class);
                    String updatedCode = fixingResponse.updatedCode().trim();

                    String oldCode = Files.readString(metadata.htmlPath());
                    Map<String, String> oldComponents = gameStorageService.getGameComponentContent(run.getGameMetadata().runId());

                    Files.createDirectories(metadata.htmlPath().getParent());
                    Files.writeString(metadata.htmlPath(), updatedCode);

                    // Stream updated code to IDE
                    Map<String, String> newComponents = gameStorageService.getGameComponentContent(run.getGameMetadata().runId());
                    if (newComponents != null && oldComponents != null) {
                        ideSseService.streamFileUpdate("html", oldComponents.get("html"), newComponents.get("html"));
                        ideSseService.streamFileUpdate("css", oldComponents.get("css"), newComponents.get("css"));
                        ideSseService.streamFileUpdate("js", oldComponents.get("js"), newComponents.get("js"));
                    }

                    run.getGameMetadata().doneTodos().addAll(currentTodos);
                    run.getGameMetadata().todos().clear();

                    log.info("Successfully saved {} characters of fixed code for run {} to {}. Summary: {}", 
                            updatedCode.length(), metadata.runId(), metadata.htmlPath(), fixingResponse.changesSummary());
                    terminalSseService.sendTerminalText("Fixed code: "+ fixingResponse.changesSummary(), SseEventType.AGENT_WORK, 50);
                } catch (Exception e) {
                    log.error("Failed to parse or save fixed code for run {} to {}", metadata.runId(), metadata.htmlPath(), e);
                }
            } else {
                log.error("LLM fixing failed for run {}: {}", metadata.runId(), response.errorMessage());
                notificationSseService.sendNotification("Fixing failed for the tasks: " + response.errorMessage());
            }
        }

        if (run.getGameMetadata().todos().isEmpty()) {
            log.info("All To-Dos completed for run {}", run.getGameMetadata().runId());
            terminalSseService.sendTerminalText("All identified tasks have been successfully processed.", SseEventType.AGENT_WORK, 50);
        } else {
            log.warn("Fixing phase ended with remaining To-Dos for run {}", run.getGameMetadata().runId());
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
