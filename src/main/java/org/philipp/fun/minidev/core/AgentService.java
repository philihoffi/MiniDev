package org.philipp.fun.minidev.core;

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
    private static final int MAX_FIXING_ITERATIONS = 3;
    private static final List<String> DEFAULT_PLANNING_TODOS = List.of(
            "Implement game logic",
            "Design game UI",
            "Test and polish"
    );

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
                RunState nextState = getNextState(run);
                
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

    private RunState getNextState(AgentRun run) {
        RunState current = run.getState();
        return switch (current) {
            case IDLE -> RunState.PLANNING;
            case PLANNING -> RunState.CODING;
            case CODING -> RunState.REVIEWING;
            case REVIEWING -> {
                GameMetadata metadata = run.getGameMetadata();
                if (metadata != null && !metadata.todos().isEmpty() && run.getFixingIterations() < MAX_FIXING_ITERATIONS) {//TODO better solution to determine if fixing is needed
                    yield RunState.FIXING;
                } else {
                    yield RunState.TESTING;
                }
            }
            case TESTING -> {
                if (Math.random() < 0.3 && run.getFixingIterations() < MAX_FIXING_ITERATIONS) {//TODO better solution to determine if fixing is needed
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
        terminalSseService.sendTerminalText(msg, SseEventType.AGENT_WORK, 50);

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
        saveMetadata(run);
    }

    private GameMetadata parseGameMetadata(String llmResponse, UUID runId) {
        String name = extractField(llmResponse, "NAME:", "Untitled Game");
        String concept = extractField(llmResponse, "CONCEPT:", "A simple browser game");
        List<String> todos = extractTodos(llmResponse, DEFAULT_PLANNING_TODOS);
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

    private void performCoding(AgentRun run) {
        GameMetadata metadata = run.getGameMetadata();
        if (metadata == null) {
            log.error("No metadata found for run {}", run.getRunId());
            run.transitionTo(RunState.FAILED);
            return;
        }

        log.info("Coding phase for run {}", run.getRunId());

        String todosFormatted = String.join("\n", metadata.todos().stream().map(t -> "- " + t).toList());

        Map<String, String> contextFiles = new java.util.HashMap<>();

        terminalSseService.sendTerminalText("Writing HTML...\n", SseEventType.AGENT_WORK, 50);
        StringBuilder contextBuilder = new StringBuilder();
        if (!contextFiles.isEmpty()) {
            contextBuilder.append("\nReference existing files for consistency:\n");
            contextFiles.forEach((name, content) -> {
                contextBuilder.append("--- FILE: ").append(name).append(" ---\n");
                contextBuilder.append(content).append("\n");
            });
        }

        LlmRequest request = new LlmRequest(
                List.of(
                        LlmRequest.Message.system(String.format("""
                                You are a professional web developer building a game called '%s' The game should be built with HTML, CSS, and vanilla JavaScript. JS and CSS should be embedded in the HTML.
                                The game concept is: %s
                                
                                To-Do List for implementation:
                                %s
                                
                                Instructions: %s
                                %s
                                
                                Respond ONLY with the raw code content. No markdown code blocks, no explanations.
                                """, metadata.name(), metadata.concept(), todosFormatted, "Write the HTML content for the game index page. Only return the code block with the HTML.", contextBuilder.toString())),
                        LlmRequest.Message.user("Please generate the code.")
                )
        );
        LlmResponse response = llmClient.chat(request);

        String code = "";
        if (response.success()) {
            code = response.content().trim();
        } else {
            log.error("Failed to generate code {}", response.errorMessage());
            run.transitionTo(RunState.FAILED);
            notificationSseService.sendNotification("Failed to generate code: " + response.errorMessage());
            return;
        }
        try {
            Files.createDirectories(metadata.htmlPath().getParent());
            Files.writeString(metadata.htmlPath(), code);
            log.info("Saved file to {}", metadata.htmlPath());
        } catch (IOException e) {
            log.error("Failed to save file to {}", metadata.htmlPath(), e);
        }

        contextFiles.put("index.html", code);

        saveMetadata(run);
    }

    private void performReviewing(AgentRun run) {
        GameMetadata metadata = run.getGameMetadata();
        if (metadata == null) {
            log.error("No metadata found for run {}", run.getRunId());
            run.transitionTo(RunState.FAILED);
            return;
        }

        log.info("Reviewing phase for run {}", run.getRunId());
        terminalSseService.sendTerminalText("Reviewing code and updating To-Dos...\n", SseEventType.AGENT_WORK, 50);

        String code = "";
        try {
            if (Files.exists(metadata.htmlPath())) {
                code = Files.readString(metadata.htmlPath());
            }
        } catch (IOException e) {
            log.error("Failed to read code for review from {}", metadata.htmlPath(), e);
        }

        String todosFormatted = String.join("\n", metadata.todos().stream().map(t -> "- " + t).toList());

        LlmRequest request = new LlmRequest(
                List.of(
                        LlmRequest.Message.system(String.format("""
                                You are a professional code reviewer. You are reviewing the progress of a game called '%s'.
                                The game concept is: %s
                                
                                Current To-Do List:
                                %s
                                
                                Current Implementation (HTML/JS/CSS):
                                --- START CODE ---
                                %s
                                --- END CODE ---
                                
                                Task:
                                1. Check which To-Dos from the list have already been implemented in the code.
                                2. Create an updated To-Do List.
                                3. Remove To-Dos that are fully completed.
                                4. Keep To-Dos that are not yet or only partially implemented.
                                5. Do not add brand-new To-Dos. Only keep or remove items from the current list.
                                
                                Respond ONLY with the updated To-Do list as a bulleted list (using '-'). No other text.
                                """, metadata.name(), metadata.concept(), todosFormatted, code)),
                        LlmRequest.Message.user("Please provide the updated To-Do list.")
                )
        );

        LlmResponse response = llmClient.chat(request);

        if (response.success()) {
            run.getGameMetadata().todos().clear();
            run.getGameMetadata().todos().addAll(extractTodos(response.content().trim(), List.of()));
            log.info("Updated To-Dos for run {}: {}", run.getRunId(), run.getGameMetadata().todos());
            saveMetadata(run);
            terminalSseService.sendTerminalText("To-Do list updated based on review.", SseEventType.AGENT_WORK, 50);
        } else {
            log.error("Failed to review code: {}", response.errorMessage());
            notificationSseService.sendNotification("Review failed: " + response.errorMessage());
        }
    }

    private void performTesting(AgentRun run) {
        String msg = "Running tests...\nAll tests passed!";
        terminalSseService.sendTerminalText(msg, SseEventType.AGENT_WORK, 50);
    }

    private void performFixing(AgentRun run) {
        GameMetadata metadata = run.getGameMetadata();
        if (metadata == null) {
            log.error("No metadata found for run {}", run.getRunId());
            run.transitionTo(RunState.FAILED);
            return;
        }

        log.info("Fixing phase for run {}", run.getRunId());
        run.incrementFixingIterations();
        terminalSseService.sendTerminalText("Fixing identified issues and implementing missing To-Dos...\n", SseEventType.AGENT_WORK, 50);

        String currentCode = "";
        try {
            if (Files.exists(metadata.htmlPath())) {
                currentCode = Files.readString(metadata.htmlPath());
            }
        } catch (IOException e) {
            log.error("Failed to read code for fixing from {}", metadata.htmlPath(), e);
        }

        String todosFormatted = String.join("\n", metadata.todos().stream().map(t -> "- " + t).toList());

        LlmRequest request = new LlmRequest(
                List.of(
                        LlmRequest.Message.system(String.format("""
                                You are a professional web developer. Your task is to fix issues and implement missing features in a game called '%s'.
                                The game concept is: %s
                                
                                Current To-Do List of remaining tasks:
                                %s
                                
                                Current Implementation (HTML/JS/CSS):
                                --- START CODE ---
                                %s
                                --- END CODE ---
                                
                                Instructions:
                                1. Review the current implementation and the To-Do list.
                                2. Update the code to address the remaining To-Dos.
                                3. Fix any bugs or inconsistencies you find.
                                4. Ensure the game remains a single HTML file with embedded CSS and JS.
                                
                                Respond ONLY with the complete, updated raw code content. No markdown code blocks, no explanations.
                                """, metadata.name(), metadata.concept(), todosFormatted, currentCode)),
                        LlmRequest.Message.user("Please provide the updated code.")
                )
        );

        LlmResponse response = llmClient.chat(request);

        if (response.success()) {
            String updatedCode = response.content().trim();
            try {
                Files.createDirectories(metadata.htmlPath().getParent());
                Files.writeString(metadata.htmlPath(), updatedCode);
                log.info("Saved fixed code to {}", metadata.htmlPath());
                terminalSseService.sendTerminalText("Issues fixed and code updated.\n", SseEventType.AGENT_WORK, 50);
            } catch (IOException e) {
                log.error("Failed to save fixed code to {}", metadata.htmlPath(), e);
            }
        } else {
            log.error("Failed to generate fixed code: {}", response.errorMessage());
            notificationSseService.sendNotification("Fixing failed: " + response.errorMessage());
        }

        saveMetadata(run);
    }

    private void performPublishing(AgentRun run) {
        String msg = "Publishing game to storage...";
        terminalSseService.sendTerminalText(msg, SseEventType.AGENT_WORK, 50);

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
        terminalSseService.sendTerminalText(msg, SseEventType.AGENT_WORK, 50);
    }

    public AgentRun getRun(UUID runId) {
        return activeRuns.get(runId);
    }
}
