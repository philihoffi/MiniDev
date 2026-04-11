package org.philipp.fun.minidev.core;

import org.philipp.fun.minidev.run.AgentRun;
import org.philipp.fun.minidev.run.AgentRun.RunState;
import org.philipp.fun.minidev.web.NotificationSseService;
import org.philipp.fun.minidev.web.TerminalSseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final Map<UUID, AgentRun> activeRuns = new ConcurrentHashMap<>();
    private final NotificationSseService notificationSseService;
    private final TerminalSseService terminalSseService;

    public AgentService(NotificationSseService notificationSseService, TerminalSseService terminalSseService) {
        this.notificationSseService = notificationSseService;
        this.terminalSseService = terminalSseService;
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
    }

    private void performCoding(AgentRun run) {
        String msg = "Writing HTML...\nGenerating CSS styles...\nImplementing JavaScript logic...";
        terminalSseService.sendTerminalText(msg, "agent-work", 50);
    }

    private void performReviewing(AgentRun run) {
        String msg = "Reviewing code for bugs...\nChecking standards...";
        terminalSseService.sendTerminalText(msg, "agent-work", 50);
    }

    private void performTesting(AgentRun run) {
        String msg = "Running tests...\nAll tests passed!";
        terminalSseService.sendTerminalText(msg, "agent-work", 50);
    }

    private void performFixing(AgentRun run) {
        String msg = "Fixing identified issues...";
        terminalSseService.sendTerminalText(msg, "agent-work", 50);
    }

    private void performPublishing(AgentRun run) {
        String msg = "Publishing game to storage...";
        terminalSseService.sendTerminalText(msg, "agent-work", 50);
    }

    private void performDone(AgentRun run) {
        String msg = "Work complete!";
        terminalSseService.sendTerminalText(msg, "agent-work", 50);
    }

    public AgentRun getRun(UUID runId) {
        return activeRuns.get(runId);
    }
}
