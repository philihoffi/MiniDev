package org.philipp.fun.minidev.intent;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record IntentRequest(
        @NotNull IntentType intent,
        @NotNull UUID runId,
        @NotNull Instant requestedAt
) {

    public IntentRequest(IntentType intent, UUID runId) {
        this(intent, runId, Instant.now());
    }


    public enum IntentType {
        START_WORK,
        CONTINUE_WORK,
        PAUSE_WORK,
        GENERATE_NEW_GAME,
        IMPROVE_CURRENT_GAME
    }
}
