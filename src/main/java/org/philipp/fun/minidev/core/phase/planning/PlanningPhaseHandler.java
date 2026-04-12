package org.philipp.fun.minidev.core.phase.planning;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.philipp.fun.minidev.core.phase.PhaseHandler;
import org.philipp.fun.minidev.intent.DecisionResponse;
import org.philipp.fun.minidev.intent.DecisionService;
import org.philipp.fun.minidev.intent.IdeaSelectionResponse;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class PlanningPhaseHandler implements PhaseHandler {

    private static final Logger log = LoggerFactory.getLogger(PlanningPhaseHandler.class);
    private static final List<String> DEFAULT_PLANNING_TODOS = List.of(
            "Implement game logic",
            "Design game UI",
            "Test and polish"
    );

    private final NotificationSseService notificationSseService;
    private final TerminalSseService terminalSseService;
    private final LlmClient llmClient;
    private final String storageBasePath;
    private final ObjectMapper objectMapper;
    private final DecisionService decisionService;

    public PlanningPhaseHandler(
            NotificationSseService notificationSseService,
            TerminalSseService terminalSseService,
            LlmClient llmClient,
            @Value("${minidev.storage.base-path}") String storageBasePath,
            ObjectMapper objectMapper,
            DecisionService decisionService) {
        this.notificationSseService = notificationSseService;
        this.terminalSseService = terminalSseService;
        this.llmClient = llmClient;
        this.storageBasePath = storageBasePath;
        this.objectMapper = objectMapper;
        this.decisionService = decisionService;
    }

    @Override
    public void execute(AgentRun run) {
        UUID runId = run.getGameMetadata().runId();
        log.info("Starting planning phase for run {}", runId);
        terminalSseService.sendTerminalText("Initiating brainstorming for game concepts...", SseEventType.AGENT_WORK, 30);

        List<String> previousIdeas = getPreviousIdeas();
        log.debug("Found {} previous ideas for run {}", previousIdeas.size(), runId);
        List<GameIdeaCandidate> candidates = generateCandidates(previousIdeas, runId);

        if (candidates.isEmpty()) {
            failRun(run, "Failed to generate any game idea candidates.");
            return;
        }

        IdeaSelectionResponse ideaSelection = decisionService.selectBestIdea(candidates, runId);
        GameIdeaCandidate bestCandidate = candidates.get(ideaSelection.selectedIndex());

        terminalSseService.sendTerminalText("Strategy selected: " + bestCandidate.name() + " (" + ideaSelection.message() + ")", SseEventType.AGENT_WORK, 30);

        GameMetadata metadata = expandIdea(bestCandidate, run.getGameMetadata().runId());
        if (metadata == null) {
            failRun(run, "Failed to expand the selected game idea.");
            return;
        }

        run.setGameMetadata(metadata);
        log.info("Initialized metadata for run {}: {}", run.getGameMetadata().runId(), metadata);
        terminalSseService.sendTerminalText("Game architecture and planning finalized: " + metadata.name(), SseEventType.AGENT_WORK, 100);
    }

    private List<String> getPreviousIdeas() {
        Path root = Paths.get(storageBasePath);
        if (!Files.exists(root)) return List.of();

        try (Stream<Path> paths = Files.list(root)) {
            return paths.filter(Files::isDirectory)
                    .map(p -> p.resolve("metadata.json"))
                    .filter(Files::exists)
                    .map(p -> {
                        try {
                            return objectMapper.readTree(p.toFile()).get("concept").asText();
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.warn("Could not read previous ideas: {}", e.getMessage());
            return List.of();
        }
    }

    private List<GameIdeaCandidate> generateCandidates(List<String> previousIdeas, UUID runId) {
        log.info("Generating game candidates for run {}", runId);
        String previousIdeasContext = previousIdeas.isEmpty() ? "None." : String.join("\n---\n", previousIdeas);

        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "ideas", Map.of(
                                "type", "array",
                                "items", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "name", Map.of("type", "string", "description", "A catchy, short title for the game."),
                                                "hook", Map.of("type", "string", "description", "A one-sentence 'elevator pitch' that grab's the player's attention."),
                                                "coreMechanic", Map.of("type", "string", "description", "A technical but brief description of the primary gameplay loop."),
                                                "uniqueness", Map.of("type", "string", "description", "What makes this game different from any other?"),
                                                "similarityRisk", Map.of("type", "string", "description", "List any existing games that might be similar and how this idea differs."),
                                                "feasibility", Map.of("type", "integer", "description", "Scale 1-10: How easily can this be implemented in a single-file HTML/JS/CSS?"),
                                                "originalityScore", Map.of("type", "integer", "description", "Scale 1-10: How unique is the core mechanic?")
                                        ),
                                        "required", List.of("name", "hook", "coreMechanic", "uniqueness", "similarityRisk", "feasibility", "originalityScore"),
                                        "additionalProperties", false
                                )
                        )
                ),
                "required", List.of("ideas"),
                "additionalProperties", false
        );

        LlmRequest request = new LlmRequest(List.of(
                LlmRequest.Message.system("""
                        You are a visionary game designer at a cutting-edge experimental game studio.
                        Your mission is to brainstorm 6-10 radical, non-derivative browser game concepts.
                        
                        CRITICAL GUIDELINES:
                        - NO CLONES: No Clone-like game that is already out there.
                        - MECHANICAL INNOVATION: Focus on a "twist" or a completely new way to interact (e.g., using only one button in a non-obvious way, time-manipulation, physics-based puzzles, etc.).
                        - SINGLE-FILE FEASIBILITY: The game must be playable in a modern browser using only a single HTML file (HTML5, CSS3, Vanilla ES6+ JS). No external assets (use Canvas, SVG, or CSS for graphics).
                        - ELEGANCE: Small scope, high impact. The core loop should be understandable in seconds but offer depth.
                        
                        PREVIOUS IDEAS (DO NOT REPEAT):
                        """ + previousIdeasContext + """
                        
                        Format your response as a valid JSON object.
                        """),
                LlmRequest.Message.user("Brainstorm 6-10 highly original and innovative browser game concepts.")
        ), schema);

        LlmResponse response = llmClient.chat(request);
        if (!response.success()) {
            log.error("LLM brainstorming failed: {}", response.errorMessage());
            return List.of();
        }

        try {
            String content = cleanJsonResponse(response.content());
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(content);
            com.fasterxml.jackson.databind.JsonNode ideasNode = root.get("ideas");
            return objectMapper.readValue(ideasNode.toString(), new TypeReference<List<GameIdeaCandidate>>() {});
        } catch (Exception e) {
            log.error("Failed to parse game candidates for run {}: {}", runId, e.getMessage());
            log.debug("Raw brainstorming response for run {}: {}", runId, response.content());
            return List.of();
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

    private GameMetadata expandIdea(GameIdeaCandidate candidate, UUID runId) {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "name", Map.of("type", "string"),
                        "concept", Map.of("type", "string", "description", "Detailed narrative and gameplay overview."),
                        "coreMechanic", Map.of("type", "string", "description", "In-depth explanation of the main interaction loop."),
                        "visualStyle", Map.of("type", "string", "description", "Description of the UI/UX, colors, and overall aesthetic (must be CSS/Canvas achievable)."),
                        "todos", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string"),
                                "description", "Step-by-step implementation plan (at least 5 items)."
                        )
                ),
                "required", List.of("name", "concept", "coreMechanic", "visualStyle", "todos"),
                "additionalProperties", false
        );

        LlmRequest request = new LlmRequest(List.of(
                LlmRequest.Message.system("""
                        You are a Lead Game Architect. Expand the provided game idea into a comprehensive technical and design specification.
                        Your goal is to provide enough detail so that a developer can implement the game flawlessly in a single HTML file.
                        
                        FOCUS AREAS:
                        - GAMEPLAY DEPTH: Explain the core loop, scoring, win/loss conditions, and progression.
                        - UI/UX DESIGN: Describe the layout, HUD, and feedback for player actions.
                        - TECHNICAL FEASIBILITY: Ensure everything can be built with Vanilla JS, HTML5 Canvas/DOM, and CSS3 without external assets.
                        
                        Format your response as a valid JSON object.
                        """),
                LlmRequest.Message.user(String.format("""
                        EXPAND THIS IDEA:
                        Name: %s
                        Hook: %s
                        Core Mechanic: %s
                        """, candidate.name(), candidate.hook(), candidate.coreMechanic()))
        ), schema);

        LlmResponse response = llmClient.chat(request);
        if (!response.success()) {
            log.error("LLM expansion failed: {}", response.errorMessage());
            return null;
        }

        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(cleanJsonResponse(response.content()));
            String name = root.get("name").asText();
            String concept = root.get("concept").asText() + "\n\nVISUAL STYLE:\n" + root.get("visualStyle").asText();
            String coreMechanic = root.get("coreMechanic").asText();
            List<String> todos = objectMapper.readValue(root.get("todos").toString(), new TypeReference<List<String>>() {});

            Path gameDirectory = Paths.get(storageBasePath, "run-" + runId);
            return new GameMetadata(runId, name, concept, coreMechanic, todos, new ArrayList<>(), gameDirectory);
        } catch (Exception e) {
            log.error("Failed to parse expanded game metadata for run {}: {}", runId, e.getMessage());
            log.debug("Raw expansion response for run {}: {}", runId, response.content());
            return null;
        }
    }

    private void failRun(AgentRun run, String errorMsg) {
        log.error(errorMsg);
        run.transitionTo(RunState.FAILED);
        notificationSseService.sendNotification(errorMsg);
    }
}
