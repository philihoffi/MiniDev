package org.philipp.fun.minidev.intent;

import jakarta.validation.constraints.NotNull;
import org.philipp.fun.minidev.run.AgentRun.RunState;

public record DecisionResponse(
        @NotNull String runId,
        @NotNull RunState newState,
        @NotNull String message,
        boolean accepted
) {
}
