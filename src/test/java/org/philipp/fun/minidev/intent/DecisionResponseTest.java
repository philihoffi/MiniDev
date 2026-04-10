package org.philipp.fun.minidev.intent;

import org.junit.jupiter.api.Test;
import org.philipp.fun.minidev.run.AgentRun.RunState;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecisionResponseTest {

    @Test
    void constructorStoresAllProvidedValues() {
        UUID runId = UUID.randomUUID();
        RunState newState = RunState.PLANNING;
        String message = "Starting work on new game";
        boolean accepted = true;

        DecisionResponse response = new DecisionResponse(runId, newState, message, accepted);

        assertAll(
                () -> assertEquals(runId, response.runId()),
                () -> assertEquals(newState, response.newState()),
                () -> assertEquals(message, response.message()),
                () -> assertTrue(response.accepted())
        );
    }

    @Test
    void acceptedCanBeFalse() {
        UUID runId = UUID.randomUUID();
        RunState newState = RunState.IDLE;
        String message = "Cannot start work while already running";

        DecisionResponse response = new DecisionResponse(runId, newState, message, false);

        assertFalse(response.accepted());
    }

    @Test
    void recordsAreEqualWhenAllFieldsMatch() {
        UUID runId = UUID.randomUUID();
        RunState newState = RunState.CODING;
        String message = "Implementing feature";

        DecisionResponse response1 = new DecisionResponse(runId, newState, message, true);
        DecisionResponse response2 = new DecisionResponse(runId, newState, message, true);

        assertAll(
                () -> assertEquals(response1, response2),
                () -> assertEquals(response1.hashCode(), response2.hashCode())
        );
    }

    @Test
    void recordsAreNotEqualWhenFieldsDiffer() {
        UUID runId = UUID.randomUUID();
        String message = "Working";

        DecisionResponse response1 = new DecisionResponse(runId, RunState.PLANNING, message, true);
        DecisionResponse response2 = new DecisionResponse(runId, RunState.CODING, message, true);

        assertNotEquals(response1, response2);
    }
}
