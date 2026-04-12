package org.philipp.fun.minidev.core.phase.coding;

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
import java.util.List;
import java.util.Map;

@Component
public class CodingPhaseHandler implements PhaseHandler {

    private static final Logger log = LoggerFactory.getLogger(CodingPhaseHandler.class);

    private final NotificationSseService notificationSseService;
    private final TerminalSseService terminalSseService;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public CodingPhaseHandler(
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

        log.info("Coding phase for run {}", run.getGameMetadata().runId());

        String todosFormatted = String.join("\n", metadata.todos().stream().map(t -> "- " + t).toList());

        // Step 1: Technical Concept
        terminalSseService.sendTerminalText("Designing technical implementation for " + metadata.name() + "...", SseEventType.AGENT_WORK, 30);
        String techConcept = generateTechnicalConcept(metadata, todosFormatted);
        if (techConcept == null) {
            failRun(run, "Failed to design technical concept.");
            return;
        }

        // Step 2: Code Generation
        terminalSseService.sendTerminalText("Writing game code (HTML, CSS, JS)...", SseEventType.AGENT_WORK, 70);
        String code = generateGameCode(metadata, todosFormatted, techConcept);
        if (code == null) {
            failRun(run, "Failed to generate game code.");
            return;
        }

        try {
            Files.createDirectories(metadata.htmlPath().getParent());
            Files.writeString(metadata.htmlPath(), code);
            log.info("Saved file to {}", metadata.htmlPath());
            terminalSseService.sendTerminalText("Code successfully written to " + metadata.htmlPath().getFileName(), SseEventType.AGENT_WORK, 100);
        } catch (IOException e) {
            log.error("Failed to save file to {}", metadata.htmlPath(), e);
            failRun(run, "Failed to save the generated file: " + e.getMessage());
        }
    }

    private String generateTechnicalConcept(GameMetadata metadata, String todosFormatted) {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "technicalDesign", Map.of("type", "string"),
                        "stateManagement", Map.of("type", "string"),
                        "renderLoop", Map.of("type", "string"),
                        "inputHandling", Map.of("type", "string")
                ),
                "required", List.of("technicalDesign", "stateManagement", "renderLoop", "inputHandling"),
                "additionalProperties", false
        );

        LlmRequest request = new LlmRequest(List.of(
                LlmRequest.Message.system("""
                        You are a Lead Game Architect. Create a technical design for a browser game.
                        Focus on:
                        1. Clean architecture (separation of concerns).
                        2. Performance (efficient rendering, avoiding memory leaks).
                        3. Original implementation of mechanics (no generic boilerplate).
                        4. Modern ES6+ JavaScript features.
                        """),
                LlmRequest.Message.user(String.format("""
                        GAME: %s
                        CONCEPT: %s
                        TODOS:
                        %s
                        
                        Design the technical implementation.
                        """, metadata.name(), metadata.concept(), todosFormatted))
        ), schema);

        LlmResponse response = llmClient.chat(request);
        if (!response.success()) {
            log.error("Failed to generate technical concept: {}", response.errorMessage());
            return null;
        }

        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(cleanJsonResponse(response.content()));
            return String.format("""
                    TECHNICAL DESIGN: %s
                    STATE MANAGEMENT: %s
                    RENDER LOOP: %s
                    INPUT HANDLING: %s
                    """,
                    node.get("technicalDesign").asText(),
                    node.get("stateManagement").asText(),
                    node.get("renderLoop").asText(),
                    node.get("inputHandling").asText()
            );
        } catch (Exception e) {
            log.error("Failed to parse technical concept: {}", e.getMessage());
            return response.content(); // Fallback to raw if parsing fails but technically it shouldn't with structured output
        }
    }

    private String generateGameCode(GameMetadata metadata, String todosFormatted, String techConcept) {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "code", Map.of("type", "string"),
                        "complexityNotes", Map.of("type", "string")
                ),
                "required", List.of("code"),
                "additionalProperties", false
        );

        LlmRequest request = new LlmRequest(List.of(
                LlmRequest.Message.system(String.format("""
                        You are a Professional Web Game Developer. Implement the game '%s'.
                        
                        REQUIREMENTS:
                        - Single file HTML solution (CSS and JS embedded).
                        - Use Vanilla JS (ES6+), NO external libraries.
                        - High-quality, polished UI and animations.
                        - Responsive design.
                        - Clean, documented code.
                        - Implementation MUST follow the provided technical concept.
                        
                        TECHNICAL CONCEPT:
                        %s
                        """, metadata.name(), techConcept)),
                LlmRequest.Message.user(String.format("""
                        CONCEPT: %s
                        TODOS:
                        %s
                        
                        Generate the complete code for the game.
                        """, metadata.concept(), todosFormatted))
        ), schema);

        LlmResponse response = llmClient.chat(request);
        if (!response.success()) {
            log.error("Failed to generate code: {}", response.errorMessage());
            return null;
        }

        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(cleanJsonResponse(response.content()));
            return node.get("code").asText().trim();
        } catch (Exception e) {
            log.error("Failed to parse game code JSON: {}", e.getMessage());
            return cleanJsonResponse(response.content());
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

    private void failRun(AgentRun run, String message) {
        log.error("Run {} failed: {}", run.getGameMetadata().runId(), message);
        run.transitionTo(RunState.FAILED);
        notificationSseService.sendNotification("Phase CODING failed: " + message);
        terminalSseService.sendTerminalText("ERROR: " + message + "\n", SseEventType.AGENT_WORK, 0);
    }
}
