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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
                                                "name", Map.of("type", "string"),
                                                "hook", Map.of("type", "string"),
                                                "coreMechanic", Map.of("type", "string"),
                                                "uniqueness", Map.of("type", "string"),
                                                "similarityRisk", Map.of("type", "string"),
                                                "feasibility", Map.of("type", "integer"),
                                                "originalityScore", Map.of("type", "integer")
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
                        You are a highly creative game designer specializing in original, non-derivative mechanics.
                        Generate 6-10 unique browser game ideas.
                        
                        CRITICAL CONSTRAINTS:
                        - NO common patterns: platformers, clickers, endless runners, snake-like, flappy-like, shooters, match-3, tower defense, or standard RPGs.
                        - Focus on ORIGINAL MECHANICS and INNOVATIVE gameplay, not just themes.
                        - Ideas MUST be significantly different from these previously generated ones:
                        """ + previousIdeasContext + """
                        
                        - Rejection Policy: Reject any idea that resembles existing popular games.
                        - Format: Respond with a JSON object containing an 'ideas' array.
                        """),
                LlmRequest.Message.user("Generate 6-10 innovative browser game concepts for HTML/JS/CSS.")
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
        LlmRequest request = new LlmRequest(List.of(
                LlmRequest.Message.system("""
                        You are a game architect. Expand the following game idea into a full technical concept.
                        
                        IDEA:
                        Name: """ + candidate.name() + """
                        Hook: """ + candidate.hook() + """
                        Core Mechanic: """ + candidate.coreMechanic() + """
                        
                        Format your response EXACTLY like this:
                        NAME: [name]
                        CONCEPT: [detailed concept, core loop, and player goal]
                        MECHANIC: [detailed description of the key mechanic]
                        TODOS:
                        - [todo 1]
                        - [todo 2]
                        - ...
                        """),
                LlmRequest.Message.user("Expand this idea into a detailed concept.")
        ));

        LlmResponse response = llmClient.chat(request);
        if (!response.success()) {
            log.error("LLM expansion failed: {}", response.errorMessage());
            return null;
        }

        return parseGameMetadata(response.content(), runId, candidate.coreMechanic());
    }

    private void failRun(AgentRun run, String errorMsg) {
        log.error(errorMsg);
        run.transitionTo(RunState.FAILED);
        notificationSseService.sendNotification(errorMsg);
    }

    private GameMetadata parseGameMetadata(String llmResponse, UUID runId, String candidateCoreMechanic) {
        String name = extractField(llmResponse, "NAME:", "Untitled Game");
        String concept = extractField(llmResponse, "CONCEPT:", "A simple browser game");
        String mechanic = extractField(llmResponse, "MECHANIC:", candidateCoreMechanic);
        List<String> todos = extractTodos(llmResponse, DEFAULT_PLANNING_TODOS);
        Path gameDirectory = Paths.get(storageBasePath, "run-" + runId);

        return new GameMetadata(runId, name, concept, mechanic, todos, new ArrayList<>(), gameDirectory);
    }

    private String extractField(String text, String marker, String defaultValue) {
        Pattern pattern = Pattern.compile(Pattern.quote(marker) + "\\s*(.+?)(?=\\n[A-Z]+:|\\nTODOS:|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return defaultValue;
    }

    private List<String> extractTodos(String text, List<String> fallbackTodos) {
        List<String> todos = new ArrayList<>();
        Pattern pattern = Pattern.compile("^\\s*-\\s*(.+)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            todos.add(matcher.group(1).trim());
        }

        if (todos.isEmpty()) {
            return new ArrayList<>(fallbackTodos);
        }

        return todos;
    }
}
