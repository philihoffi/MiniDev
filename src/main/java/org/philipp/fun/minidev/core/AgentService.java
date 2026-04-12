package org.philipp.fun.minidev.core;

import org.philipp.fun.minidev.core.phase.PhaseHandler;
import org.philipp.fun.minidev.core.phase.coding.CodingPhaseHandler;
import org.philipp.fun.minidev.core.phase.fixing.FixingPhaseHandler;
import org.philipp.fun.minidev.core.phase.planning.PlanningPhaseHandler;
import org.philipp.fun.minidev.core.phase.publishing.PublishingPhaseHandler;
import org.philipp.fun.minidev.core.phase.reviewing.ReviewingPhaseHandler;
import org.philipp.fun.minidev.core.phase.testing.TestingPhaseHandler;
import org.philipp.fun.minidev.intent.DecisionService;
import org.philipp.fun.minidev.run.AgentRun;
import org.philipp.fun.minidev.run.AgentRun.RunState;
import org.philipp.fun.minidev.run.GameMetadata;
import org.philipp.fun.minidev.web.service.NotificationSseService;
import org.philipp.fun.minidev.web.service.TerminalSseService;
import org.philipp.fun.minidev.web.service.AbstractSseService.SseEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final Map<UUID, AgentRun> activeRuns = new ConcurrentHashMap<>();
    private final NotificationSseService notificationSseService;

    private final Map<RunState, PhaseHandler> phaseHandlers;

    private final GameStorageService gameStorageService;
    private final DecisionService decisionService;

    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private UUID activeRunId = null;

    public AgentService(
            NotificationSseService notificationSseService,
            PlanningPhaseHandler planningPhaseHandler,
            CodingPhaseHandler codingPhaseHandler,
            ReviewingPhaseHandler reviewingPhaseHandler,
            TestingPhaseHandler testingPhaseHandler,
            FixingPhaseHandler fixingPhaseHandler,
            PublishingPhaseHandler publishingPhaseHandler,
            GameStorageService gameStorageService,
            DecisionService decisionService) {
        this.notificationSseService = notificationSseService;
        this.decisionService = decisionService;

        this.phaseHandlers = Map.of(
                RunState.PLANNING, planningPhaseHandler,
                RunState.CODING, codingPhaseHandler,
                RunState.REVIEWING, reviewingPhaseHandler,
                RunState.TESTING, testingPhaseHandler,
                RunState.FIXING, fixingPhaseHandler,
                RunState.PUBLISHING, publishingPhaseHandler
        );
        this.gameStorageService = gameStorageService;
    }

    public UUID startNewRun() {
        if (isProcessing.get()) {
            throw new IllegalStateException("An agent run is already in progress.");
        }
        
        AgentRun run = new AgentRun(this.gameStorageService.getStorageBasePath());
        UUID uuid = run.getGameMetadata().runId();
        activeRuns.put(uuid, run);
        log.info("Started new test run: {}", uuid);
        notificationSseService.sendNotification("New Run started: " + uuid);

        processRun(uuid);
        
        return uuid;
    }

    @Async
    protected void processRun(UUID runId) {
        if (!isProcessing.compareAndSet(false, true)) {
            log.warn("ProcessRun called while another run is already being processed.");
            return;
        }

        this.activeRunId = runId;
        AgentRun run = activeRuns.get(runId);
        if (run == null) {
            isProcessing.set(false);
            return;
        }

        log.info("Processing run {}", runId);
        
        try {
            RunState currentState = run.getState();
            while (currentState != RunState.DONE && currentState != RunState.FAILED) {
                RunState nextState = decisionService.decideNextStep(run).newState();
                
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
                gameStorageService.saveMetadata(run);
            }
        } catch (InterruptedException e) {
            log.error("Run {} was interrupted", runId, e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error during run {}", runId, e);
            run.transitionTo(RunState.FAILED);
            notificationSseService.sendNotification("Run FAILED: " + e.getMessage());
        } finally {
            isProcessing.set(false);
            this.activeRunId = null;
        }
    }

    public void resumeRun(UUID runId) {
        if (isProcessing.get()) {
            throw new IllegalStateException("An agent run is already in progress.");
        }

        AgentRun run = activeRuns.get(runId);
        if (run == null) {
            run = gameStorageService.loadRunFromDisk(runId);
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
            run = gameStorageService.loadRunFromDisk(runId);
        }
        return Optional.ofNullable(run);
    }

    public UUID getActiveRunId() {
        return activeRunId;
    }
}
