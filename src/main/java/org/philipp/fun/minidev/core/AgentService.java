package org.philipp.fun.minidev.core;

import org.philipp.fun.minidev.core.phase.PhaseHandler;
import org.philipp.fun.minidev.core.phase.coding.CodingPhaseHandler;
import org.philipp.fun.minidev.core.phase.fixing.FixingPhaseHandler;
import org.philipp.fun.minidev.core.phase.planning.PlanningPhaseHandler;
import org.philipp.fun.minidev.core.phase.publishing.PublishingPhaseHandler;
import org.philipp.fun.minidev.core.phase.reviewing.ReviewingPhaseHandler;
import org.philipp.fun.minidev.core.phase.testing.TestingPhaseHandler;
import org.philipp.fun.minidev.llm.LlmClient;
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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);
    private static final int MAX_FIXING_ITERATIONS = 3;

    private final Map<UUID, AgentRun> activeRuns = new ConcurrentHashMap<>();
    private final NotificationSseService notificationSseService;

    private final Map<RunState, PhaseHandler> phaseHandlers;

    private final ObjectMapper objectMapper;

    public AgentService(
            NotificationSseService notificationSseService,
            PlanningPhaseHandler planningPhaseHandler,
            CodingPhaseHandler codingPhaseHandler,
            ReviewingPhaseHandler reviewingPhaseHandler,
            TestingPhaseHandler testingPhaseHandler,
            FixingPhaseHandler fixingPhaseHandler,
            PublishingPhaseHandler publishingPhaseHandler,
            ObjectMapper objectMapper) {
        this.notificationSseService = notificationSseService;

        this.phaseHandlers = Map.of(
                RunState.PLANNING, planningPhaseHandler,
                RunState.CODING, codingPhaseHandler,
                RunState.REVIEWING, reviewingPhaseHandler,
                RunState.TESTING, testingPhaseHandler,
                RunState.FIXING, fixingPhaseHandler,
                RunState.PUBLISHING, publishingPhaseHandler
        );
        this.objectMapper = objectMapper;
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

    private void saveMetadata(AgentRun run) {
        GameMetadata metadata = run.getGameMetadata();
        if (metadata == null) return;
        Path files = metadata.files();
        if (files == null) return;
        
        Path metadataPath = files.resolve("metadata.json");
        try {
            Files.createDirectories(files);
            this.objectMapper.writeValue(metadataPath.toFile(), metadata);
            log.info("Saved metadata for run {} to {}", run.getRunId(), metadataPath);
        } catch (IOException e) {
            log.error("Failed to save metadata for run {}", run.getRunId(), e);
        }
    }
}
