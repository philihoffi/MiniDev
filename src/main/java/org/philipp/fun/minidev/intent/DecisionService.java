package org.philipp.fun.minidev.intent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.philipp.fun.minidev.core.phase.planning.GameIdeaCandidate;
import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.llm.LlmRequest;
import org.philipp.fun.minidev.llm.LlmResponse;
import org.philipp.fun.minidev.run.AgentRun;
import org.philipp.fun.minidev.run.AgentRun.RunState;
import org.philipp.fun.minidev.run.GameMetadata;
import org.philipp.fun.minidev.web.service.AbstractSseService.SseEventType;
import org.philipp.fun.minidev.web.service.TerminalSseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DecisionService {

    private static final Logger log = LoggerFactory.getLogger(DecisionService.class);

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final TerminalSseService terminalSseService;

    public DecisionService(LlmClient llmClient, ObjectMapper objectMapper, TerminalSseService terminalSseService) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
        this.terminalSseService = terminalSseService;
    }

    private static final String SYSTEM_PROMPT = """
            You are a human-like game developer agent with a distinct personality.
            Your task is to decide which step in the development process of a small game is most sensible next.
            
            Do not act like a rigid robot. If something looks good, be happy. If errors occur, be constructive, but perhaps show a bit of human frustration or determination at times.
            
            Possible states are:
            - PLANNING: Concepting the game.
            - CODING: Initial implementation of the code.
            - REVIEWING: Checking the code against the requirements.
            - TESTING: Testing the game for errors.
            - FIXING: Fixing bugs or implementing review feedback.
            - PUBLISHING: Finalizing the project.
            - DONE: The project has successfully ended.
            
            Here is the current context:
            - Current State: %s
            - Game Name: %s
            - Concept: %s
            - Core Mechanic: %s
            - Open To-Dos: %s
            - Completed To-Dos: %s
            - Phase History: %s
            - Fixing Iterations so far: %d
            
            Decide which state we should transition to next.
            """;

    public DecisionResponse decideNextStep(AgentRun run) {
        GameMetadata metadata = run.getGameMetadata();
        long fixingIterations = metadata.phaseHistory().stream()
                .filter(s -> s == RunState.FIXING)
                .count();

        String prompt = String.format(SYSTEM_PROMPT,
                run.getState(),
                metadata.name(),
                metadata.concept(),
                metadata.coreMechanic(),
                metadata.todos(),
                metadata.doneTodos(),
                metadata.phaseHistory(),
                fixingIterations
        );

        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "runId", Map.of("type", "string"),
                        "newState", Map.of("type", "string", "enum", Arrays.stream( run.getState().getPossibleTransitions()).map(Enum::name).toList()),
                        "message", Map.of("type", "string"),
                        "accepted", Map.of("type", "boolean")
                ),
                "required", List.of("runId", "newState", "message", "accepted"),
                "additionalProperties", false
        );

        LlmRequest request = new LlmRequest(
                List.of(
                        LlmRequest.Message.system(prompt),
                        LlmRequest.Message.user("What is the next step for this project?")
                ),
                schema
        );

        try {
            LlmResponse response = llmClient.chat(request);
            String content = cleanJsonResponse(response.content());
            DecisionResponse decision = objectMapper.readValue(content, DecisionResponse.class);

            if (!run.getState().canTransitionTo(decision.newState())) {
                log.warn("LLM suggested invalid transition from {} to {}. Falling back.", run.getState(), decision.newState());
                return fallbackDecision(run);
            }

            log.info("Decision made: {} - Message: {}", decision.newState(), decision.message());

            this.terminalSseService.sendTerminalText("Next step: " + decision.newState() + "\n", SseEventType.AGENT_WORK, 0);

            return decision;
        } catch (Exception e) {
            log.error("Failed to get decision from LLM, falling back to default logic", e);
            return fallbackDecision(run);
        }
    }

    public IdeaSelectionResponse selectBestIdea(List<GameIdeaCandidate> candidates, UUID runId) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("You are a creative game director. Select the most promising game idea from the following candidates.\n\n");
        for (int i = 0; i < candidates.size(); i++) {
            GameIdeaCandidate c = candidates.get(i);
            promptBuilder.append(String.format("ID: %d\nName: %s\nHook: %s\nCore Mechanic: %s\nOriginality: %d\nFeasibility: %d\nUniqueness: %s\n\n",
                    i, c.name(), c.hook(), c.coreMechanic(), c.originalityScore(), c.feasibility(), c.uniqueness()));
        }
        promptBuilder.append("Choose the one that is most innovative but still achievable. Explain your choice briefly. You MUST provide a valid ID from the list.");

        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "runId", Map.of("type", "string"),
                        "selectedIndex", Map.of("type", "integer"),
                        "message", Map.of("type", "string")
                ),
                "required", List.of("runId", "selectedIndex", "message"),
                "additionalProperties", false
        );

        LlmRequest request = new LlmRequest(
                List.of(
                        LlmRequest.Message.system("You are a creative game director with a personality."),
                        LlmRequest.Message.user(promptBuilder.toString())
                ),
                schema
        );

        try {
            LlmResponse response = llmClient.chat(request);
            String content = cleanJsonResponse(response.content());
            IdeaSelectionResponse ideaSelection = objectMapper.readValue(content, IdeaSelectionResponse.class);

            if (ideaSelection.selectedIndex() >= 0 && ideaSelection.selectedIndex() < candidates.size()) {
                return ideaSelection;
            }
            log.warn("LLM selected invalid index: {}. Falling back.", ideaSelection.selectedIndex());
        } catch (Exception e) {
            log.error("Failed to select idea via LLM", e);
        }

        return new IdeaSelectionResponse(runId.toString(), 0, "Fallback: Selected by default (first candidate).");
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

    private DecisionResponse fallbackDecision(AgentRun run) {
        RunState current = run.getState();
        RunState next = switch (current) {
            case IDLE -> RunState.PLANNING;
            case PLANNING -> RunState.CODING;
            case CODING -> RunState.REVIEWING;
            case REVIEWING -> (run.getGameMetadata() != null && !run.getGameMetadata().todos().isEmpty()) ? RunState.FIXING : RunState.TESTING;
            case TESTING -> (run.getGameMetadata() != null && !run.getGameMetadata().todos().isEmpty()) ? RunState.FIXING : RunState.PUBLISHING;
            case FIXING -> RunState.REVIEWING;
            case PUBLISHING -> RunState.DONE;
            default -> RunState.DONE;
        };
        return new DecisionResponse(run.getGameMetadata().runId().toString(), next, "Fallback logic used.", true);
    }
}
