package org.philipp.fun.minidev.core.phase.publishing;

import org.philipp.fun.minidev.core.phase.PhaseHandler;
import org.philipp.fun.minidev.run.AgentRun;
import org.philipp.fun.minidev.web.service.TerminalSseService;
import org.philipp.fun.minidev.web.service.AbstractSseService.SseEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PublishingPhaseHandler implements PhaseHandler {

    private static final Logger log = LoggerFactory.getLogger(PublishingPhaseHandler.class);
    private final TerminalSseService terminalSseService;

    public PublishingPhaseHandler(TerminalSseService terminalSseService) {
        this.terminalSseService = terminalSseService;
    }

    @Override
    public void execute(AgentRun run) {
        UUID runId = run.getGameMetadata().runId();
        log.info("Starting publishing phase for run {}", runId);
        String msg = "Finalizing and storing game assets...";
        terminalSseService.sendTerminalText(msg, SseEventType.AGENT_WORK, 50);
        log.info("Game published for run {}", runId);
    }
}
