package org.philipp.fun.minidev.core;

import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.llm.LlmRequest;
import org.philipp.fun.minidev.llm.LlmResponse;
import org.philipp.fun.minidev.run.AgentRun;
import org.philipp.fun.minidev.run.AgentRun.RunState;
import org.philipp.fun.minidev.run.GameMetadata;
import org.philipp.fun.minidev.web.service.NotificationSseService;
import org.philipp.fun.minidev.web.service.TerminalSseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final Map<UUID, AgentRun> activeRuns = new ConcurrentHashMap<>();
    private final NotificationSseService notificationSseService;
    private final TerminalSseService terminalSseService;
    private final LlmClient llmClient;
    private final String storageBasePath;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public AgentService(
            NotificationSseService notificationSseService,
            TerminalSseService terminalSseService,
            LlmClient llmClient,
            @Value("${minidev.storage.base-path}") String storageBasePath) {
        this.notificationSseService = notificationSseService;
        this.terminalSseService = terminalSseService;
        this.llmClient = llmClient;
        this.storageBasePath = storageBasePath;
    }

    public UUID startNewRun() {
        AgentRun run = new AgentRun();
        activeRuns.put(run.getRunId(), run);
        log.info("Started new test run: {}", run.getRunId());
        notificationSseService.sendNotification("New Run started: " + run.getRunId());

        processRun(run.getRunId());
        
        return run.getRunId();
    }

    protected void processRun(UUID runId) {
        AgentRun run = activeRuns.get(runId);
        if (run == null) return;

        log.info("Processing run {}", runId);
        
        try {
            RunState currentState = run.getState();
            while (currentState != RunState.DONE && currentState != RunState.FAILED) {
                RunState nextState = getNextSimulatedState(currentState);
                
                if (run.getState().canTransitionTo(nextState)) {
                    run.transitionTo(nextState);
                    log.info("Run {} transitioned to {}", runId, nextState);
                    
                    notificationSseService.sendNotification("Phase: " + nextState);
                    performPhaseWork(run, nextState);
                    
                    Thread.sleep(2000);
                    currentState = nextState;
                } else {
                    log.warn("Cannot transition run {} from {} to {}", runId, run.getState(), nextState);
                    break;
                }
            }
        } catch (InterruptedException e) {
            log.error("Run {} was interrupted", runId, e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error during run {}", runId, e);
            run.transitionTo(RunState.FAILED);
            notificationSseService.sendNotification("Run FAILED: " + e.getMessage());
        }
    }

    private RunState getNextSimulatedState(RunState current) {
        return switch (current) {
            case IDLE -> RunState.PLANNING;
            case PLANNING -> RunState.CODING;
            case CODING -> RunState.REVIEWING;
            case REVIEWING -> RunState.TESTING;
            case TESTING -> {
                if (Math.random() < 0.3) {
                    yield RunState.FIXING;
                } else {
                    yield RunState.PUBLISHING;
                }
            }
            case FIXING -> RunState.REVIEWING;
            case PUBLISHING -> RunState.DONE;
            default -> RunState.DONE;
        };
    }

    private void performPhaseWork(AgentRun run, RunState state) {
        switch (state) {
            case PLANNING -> performPlanning(run);
            case CODING -> performCoding(run);
            case REVIEWING -> performReviewing(run);
            case TESTING -> performTesting(run);
            case FIXING -> performFixing(run);
            case PUBLISHING -> performPublishing(run);
            case DONE -> performDone(run);
            default -> log.info("No specific work for phase {}", state);
        }
    }

    private void performPlanning(AgentRun run) {
        String msg = "Planning game architecture...\nBrainstorming features...";
        terminalSseService.sendTerminalText(msg, "agent-work", 50);

        // Create LLM request to generate game concept
        LlmRequest request = new LlmRequest(
            List.of(
                LlmRequest.Message.system("""
                    You are a creative game designer. Generate a simple browser game concept.

                    Respond in this EXACT format:
                    NAME: [catchy game name]
                    CONCEPT: [brief description of the game]
                    TODOS:
                    - [todo item 1]
                    - [todo item 2]
                    - [todo item 3]
                    - [todo item n]
                    """),
                LlmRequest.Message.user("Create a fun, simple browser game that can be built with HTML, CSS, and vanilla JavaScript.")
            )
        );


        LlmResponse response = llmClient.chat(request);

        GameMetadata metadata = null;
        if (response.success()) {
            metadata = parseGameMetadata(response.content(), run.getRunId());
            log.info("Generated game concept: {}", metadata.name());
        } else {
            log.error("Failed to generate game concept: {}", response.errorMessage());
            run.transitionTo(RunState.FAILED);
            notificationSseService.sendNotification("Failed to generate game concept: " + response.errorMessage());
            return;
        }

        run.setGameMetadata(metadata);
        log.info("Initialized metadata for run {}: {}", run.getRunId(), metadata);
    }

    private GameMetadata parseGameMetadata(String llmResponse, UUID runId) {
        String name = extractField(llmResponse, "NAME:", "Untitled Game");
        String concept = extractField(llmResponse, "CONCEPT:", "A simple browser game");
        List<String> todos = extractTodos(llmResponse);
        Path gameDirectory = Paths.get(storageBasePath, "run-" + runId);

        return new GameMetadata(
            name,
            concept,
            todos,
            gameDirectory
        );
    }

    private String extractField(String text, String marker, String defaultValue) {
        Pattern pattern = Pattern.compile(Pattern.quote(marker) + "\\s*(.+?)(?=\\n[A-Z]+:|\\nTODOS:|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return defaultValue;
    }

    private List<String> extractTodos(String text) {
        List<String> todos = new ArrayList<>();
        Pattern pattern = Pattern.compile("^\\s*-\\s*(.+)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            todos.add(matcher.group(1).trim());
        }

        if (todos.isEmpty()) {
            todos.add("Implement game logic");
            todos.add("Design game UI");
            todos.add("Test and polish");
        }

        return todos;
    }

    private void performCoding(AgentRun run) {
        String msg = "Writing HTML...\nGenerating CSS styles...\nImplementing JavaScript logic...";
        terminalSseService.sendTerminalText(msg, "agent-work", 50);

        log.info("Coding phase for run {}", run.getRunId());

        saveMetadata(run);
    }

    private void performReviewing(AgentRun run) {
        String msg = "Reviewing code for bugs...\nChecking standards...";
        terminalSseService.sendTerminalText(msg, "agent-work", 50);

        saveMetadata(run);
    }

    private void performTesting(AgentRun run) {
        String msg = "Running tests...\nAll tests passed!";
        terminalSseService.sendTerminalText(msg, "agent-work", 50);
    }

    private void performFixing(AgentRun run) {
        String msg = "Fixing identified issues...";
        terminalSseService.sendTerminalText(msg, "agent-work", 50);

        saveMetadata(run);
    }

    private void performPublishing(AgentRun run) {
        String msg = "Publishing game to storage...";
        terminalSseService.sendTerminalText(msg, "agent-work", 50);

        saveMetadata(run);
    }

    private void saveMetadata(AgentRun run) {
        Path files = run.getGameMetadata().files();
        Path metadataPath = files.resolve("metadata.json");
        try {
            Files.createDirectories(files);
            objectMapper.writeValue(metadataPath.toFile(), run.getGameMetadata());
            log.info("Saved metadata for run {} to {}", run.getRunId(), metadataPath);
        } catch (IOException e) {
            log.error("Failed to save metadata for run {}", run.getRunId(), e);
        }
    }

    public GameMetadata loadMetadata(UUID runId) {
        Path metadataPath = Paths.get(storageBasePath, "run-" + runId, "metadata.json");
        try {
            if (Files.exists(metadataPath)) {
                return objectMapper.readValue(metadataPath.toFile(), GameMetadata.class);
            }
        } catch (IOException e) {
            log.error("Failed to load metadata for run {}", runId, e);
        }
        return null;
    }

    private void performDone(AgentRun run) {
        String msg = "Work complete!";
        terminalSseService.sendTerminalText(msg, "agent-work", 50);
    }

    public AgentRun getRun(UUID runId) {
        return activeRuns.get(runId);
    }
}
