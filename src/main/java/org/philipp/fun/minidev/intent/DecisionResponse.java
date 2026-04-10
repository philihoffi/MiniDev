package org.philipp.fun.minidev.intent;

import jakarta.validation.constraints.NotNull;
import org.philipp.fun.minidev.run.AgentRun.RunState;

import java.util.UUID;

public record DecisionResponse(
        @NotNull UUID runId,
        @NotNull RunState newState,
        @NotNull String message,
        boolean accepted
) {
}
