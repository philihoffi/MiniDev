package org.philipp.fun.minidev.core;

import org.philipp.fun.minidev.core.phase.PhaseHandler;
import org.philipp.fun.minidev.core.phase.coding.CodingPhaseHandler;
import org.philipp.fun.minidev.core.phase.fixing.FixingPhaseHandler;
import org.philipp.fun.minidev.core.phase.planning.PlanningPhaseHandler;
import org.philipp.fun.minidev.core.phase.publishing.PublishingPhaseHandler;
import org.philipp.fun.minidev.core.phase.reviewing.ReviewingPhaseHandler;
import org.philipp.fun.minidev.core.phase.testing.TestingPhaseHandler;
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
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);
    private static final int MAX_FIXING_ITERATIONS = 3;

    private final Map<UUID, AgentRun> activeRuns = new ConcurrentHashMap<>();
    private final NotificationSseService notificationSseService;
    private final TerminalSseService terminalSseService;

    private final Map<RunState, PhaseHandler> phaseHandlers;

    private final ObjectMapper objectMapper;
    private final String storageBasePath;

    public AgentService(
            NotificationSseService notificationSseService,
            TerminalSseService terminalSseService,
            PlanningPhaseHandler planningPhaseHandler,
            CodingPhaseHandler codingPhaseHandler,
            ReviewingPhaseHandler reviewingPhaseHandler,
            TestingPhaseHandler testingPhaseHandler,
            FixingPhaseHandler fixingPhaseHandler,
            PublishingPhaseHandler publishingPhaseHandler,
            ObjectMapper objectMapper,
            @Value("${minidev.storage.base-path}") String storageBasePath) {
        this.notificationSseService = notificationSseService;
        this.terminalSseService = terminalSseService;

        this.phaseHandlers = Map.of(
                RunState.PLANNING, planningPhaseHandler,
                RunState.CODING, codingPhaseHandler,
                RunState.REVIEWING, reviewingPhaseHandler,
                RunState.TESTING, testingPhaseHandler,
                RunState.FIXING, fixingPhaseHandler,
                RunState.PUBLISHING, publishingPhaseHandler
        );
        this.objectMapper = objectMapper;
        this.storageBasePath = storageBasePath;
    }

    public UUID startNewRun() {
        AgentRun run = new AgentRun(this.storageBasePath);
        UUID uuid = run.getGameMetadata().runId();
        activeRuns.put(uuid, run);
        log.info("Started new test run: {}", uuid);
        notificationSseService.sendNotification("New Run started: " + uuid);

        processRun(uuid);
        
        return uuid;
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
                    PhaseHandler handler = this.phaseHandlers.get(nextState);
                    if (handler != null) {
                        handler.execute(run);
                        Thread.sleep(2000);
                    }
                    
                    currentState = nextState;
                } else {
                    log.warn("Cannot transition run {} from {} to {}", runId, run.getState(), nextState);
                    break;
                }
                saveMetadata(run);
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
                if(metadata != null && !metadata.todos().isEmpty() && run.getFixingIterations() >= MAX_FIXING_ITERATIONS){
                    metadata.todos().clear();
                    this.terminalSseService.sendTerminalText("Fuck it I'm done", SseEventType.AGENT_WORK, 0);
                }

                if (metadata != null && !metadata.todos().isEmpty()) {//TODO better solution to determine if fixing is needed
                    yield RunState.FIXING;
                } else {
                    yield RunState.TESTING;
                }
            }
            case TESTING -> {
                GameMetadata metadata = run.getGameMetadata();
                if (metadata != null && !metadata.todos().isEmpty() && run.getFixingIterations() < MAX_FIXING_ITERATIONS) {
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

    private void saveMetadata(AgentRun run) {
        GameMetadata metadata = run.getGameMetadata();
        if (metadata == null) return;
        Path files = metadata.files();
        if (files == null) return;
        
        Path metadataPath = files.resolve("metadata.json");
        try {
            Files.createDirectories(files);
            this.objectMapper.writeValue(metadataPath.toFile(), metadata);
            log.info("Saved metadata for run {} to {}", run.getGameMetadata().runId(), metadataPath);
        } catch (IOException e) {
            log.error("Failed to save metadata for run {}", run.getGameMetadata().runId(), e);
        }
    }

    public List<GameMetadata> getAllGames() {
        Path basePath = Paths.get(storageBasePath);
        if (!Files.exists(basePath) || !Files.isDirectory(basePath)) {
            return List.of();
        }

        try (Stream<Path> walk = Files.walk(basePath, 1)) {
            return walk
                    .filter(Files::isDirectory)
                    .filter(path -> !path.equals(basePath))
                    .map(path -> path.resolve("metadata.json"))
                    .filter(Files::exists)
                    .map(metadataPath -> {
                        try {
                            GameMetadata metadata = objectMapper.readValue(metadataPath.toFile(), GameMetadata.class);
                            if (metadata.runId() == null) {
                                String dirName = metadataPath.getParent().getFileName().toString();
                                if (dirName.startsWith("run-")) {
                                    try {
                                        UUID runId = UUID.fromString(dirName.substring(4));
                                        metadata = new GameMetadata(runId, metadata.name(), metadata.concept(), metadata.coreMechanic(), metadata.todos(), metadata.doneTodos(), metadata.files(), metadata.htmlPath(), metadata.readmePath());
                                    } catch (IllegalArgumentException ignored) {}
                                }
                            }
                            return metadata;
                        } catch (IOException e) {
                            log.error("Failed to load metadata from {}", metadataPath, e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException e) {
            log.error("Failed to list games in {}", storageBasePath, e);
            return List.of();
        }
    }

    public void resumeRun(UUID runId) {
        AgentRun run = activeRuns.get(runId);
        if (run == null) {
            run = loadRunFromDisk(runId);
        }

        if (run == null) {
            throw new IllegalArgumentException("Run not found: " + runId);
        }

        if (run.getState().isTerminal()) {
            run.transitionTo(RunState.REVIEWING);
        }

        activeRuns.put(runId, run);
        log.info("Resuming run: {}", runId);
        notificationSseService.sendNotification("Resuming run: " + runId);

        processRun(runId);
    }

    public Optional<AgentRun> getRun(UUID runId) {
        AgentRun run = activeRuns.get(runId);
        if (run == null) {
            run = loadRunFromDisk(runId);
        }
        return Optional.ofNullable(run);
    }

    public String getGameContent(UUID runId) {
        Path runPath = Paths.get(storageBasePath, "run-" + runId, "index.html");
        if (Files.exists(runPath)) {
            try {
                return Files.readString(runPath);
            } catch (IOException e) {
                log.error("Failed to read game content: {}", runPath, e);
            }
        }
        return null;
    }

    private AgentRun loadRunFromDisk(UUID runId) {
        Path runPath = Paths.get(storageBasePath, "run-" + runId);
        Path metadataPath = runPath.resolve("metadata.json");

        if (Files.exists(metadataPath)) {
            try {
                GameMetadata metadata = objectMapper.readValue(metadataPath.toFile(), GameMetadata.class);
                // Ensure runId in metadata matches the requested runId
                if (!runId.equals(metadata.runId())) {
                    log.warn("Run ID mismatch in metadata for run {}", runId);
                    throw new IllegalArgumentException("Run ID mismatch in metadata");
                }
                return new AgentRun(RunState.REVIEWING, Instant.now(), Instant.now(), metadata);
            } catch (IOException e) {
                log.error("Failed to load run from disk: {}", runId, e);
            }
        }
        return null;
    }
}
