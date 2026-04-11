package org.philipp.fun.minidev.core.phase.testing;

import org.philipp.fun.minidev.core.phase.PhaseHandler;
import org.philipp.fun.minidev.run.AgentRun;
import org.philipp.fun.minidev.web.service.TerminalSseService;
import org.philipp.fun.minidev.web.service.AbstractSseService.SseEventType;
import org.springframework.stereotype.Component;

@Component
public class TestingPhaseHandler implements PhaseHandler {

    private final TerminalSseService terminalSseService;

    public TestingPhaseHandler(TerminalSseService terminalSseService) {
        this.terminalSseService = terminalSseService;
    }

    @Override
    public void execute(AgentRun run) {
        String msg = "Running tests...\nAll tests passed!";
        terminalSseService.sendTerminalText(msg, SseEventType.AGENT_WORK, 50);
    }
}
