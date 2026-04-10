package org.philipp.fun.minidev.run;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;


class AgentRunTest {

    @Test
    void constructorStoresAllProvidedValues() {
        UUID runId = UUID.randomUUID();
        Instant createdAt = Instant.EPOCH;
        Instant updatedAt = createdAt.plusSeconds(30);

        AgentRun run = new AgentRun(runId, AgentRun.RunState.PLANNING, createdAt, updatedAt);

        assertAll(
                () -> assertEquals(runId, run.getRunId()),
                () -> assertEquals(AgentRun.RunState.PLANNING, run.getState()),
                () -> assertEquals(createdAt, run.getCreatedAt()),
                () -> assertEquals(updatedAt, run.getUpdatedAt())
        );
    }

    @Test
    void convenienceConstructorInitializesIdleStateAndUsesCreatedAtAsUpdatedAt() {
        UUID runId = UUID.randomUUID();
        Instant createdAt = Instant.EPOCH;

        AgentRun run = new AgentRun(runId, createdAt);

        assertAll(
                () -> assertEquals(runId, run.getRunId()),
                () -> assertEquals(AgentRun.RunState.IDLE, run.getState()),
                () -> assertEquals(createdAt, run.getCreatedAt()),
                () -> assertEquals(createdAt, run.getUpdatedAt())
        );
    }

    @Test
    void createBuildsIdleRunWithGeneratedIdAndMatchingTimestamps() {
        AgentRun run = new AgentRun();

        assertAll(
                () -> assertNotNull(run.getRunId()),
                () -> assertEquals(AgentRun.RunState.IDLE, run.getState()),
                () -> assertNotNull(run.getCreatedAt()),
                () -> assertEquals(run.getCreatedAt(), run.getUpdatedAt())
        );
    }

    @Test
    void fullConstructorRejectsNullArguments() {
        UUID runId = UUID.randomUUID();
        Instant timestamp = Instant.EPOCH;

        assertAll(
                () -> assertThrows(NullPointerException.class, () -> new AgentRun(null, AgentRun.RunState.IDLE, timestamp, timestamp)),
                () -> assertThrows(NullPointerException.class, () -> new AgentRun(runId, null, timestamp, timestamp)),
                () -> assertThrows(NullPointerException.class, () -> new AgentRun(runId, AgentRun.RunState.IDLE, null, timestamp)),
                () -> assertThrows(NullPointerException.class, () -> new AgentRun(runId, AgentRun.RunState.IDLE, timestamp, null))
        );
    }

    @Test
    void constructorRejectsUpdatedAtBeforeCreatedAt() {
        UUID runId = UUID.randomUUID();
        Instant createdAt = Instant.EPOCH;
        Instant updatedAt = createdAt.minusSeconds(1);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new AgentRun(runId, AgentRun.RunState.IDLE, createdAt, updatedAt)
        );

        assertEquals("updatedAt must not be before createdAt", exception.getMessage());
    }

    @Test
    void convenienceConstructorRejectsNullCreatedAt() {
        UUID runId = UUID.randomUUID();

        assertThrows(NullPointerException.class, () -> new AgentRun(runId, null));
    }

    @Test
    void transitionToUpdatesStateAndUpdatedAt() {
        Instant createdAt = Instant.EPOCH;
        AgentRun run = new AgentRun(UUID.randomUUID(), createdAt);
        Instant changedAt = createdAt.plusSeconds(45);

        boolean transitioned = run.transitionTo(AgentRun.RunState.CODING, changedAt);

        assertAll(
                () -> assertTrue(transitioned),
                () -> assertEquals(AgentRun.RunState.CODING, run.getState()),
                () -> assertEquals(changedAt, run.getUpdatedAt())
        );
    }

    @Test
    void transitionToRejectsInvalidArguments() {
        Instant createdAt = Instant.EPOCH;
        AgentRun run = new AgentRun(UUID.randomUUID(), createdAt);

        assertAll(
                () -> assertThrows(NullPointerException.class, () -> run.transitionTo(null, createdAt.plusSeconds(1))),
                () -> assertThrows(NullPointerException.class, () -> run.transitionTo(AgentRun.RunState.CODING, null))
        );
    }

    @Test
    void transitionToRejectsTimestampBeforeCurrentUpdatedAt() {
        Instant createdAt = Instant.EPOCH;
        AgentRun run = new AgentRun(UUID.randomUUID(), AgentRun.RunState.PLANNING, createdAt, createdAt.plusSeconds(10));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> run.transitionTo(AgentRun.RunState.CODING, createdAt.plusSeconds(9))
        );

        assertEquals("changedAt must not be before updatedAt", exception.getMessage());
    }

    @Test
    void transitionToWithoutTimestampUsesCurrentTime() {
        Instant createdAt = Instant.EPOCH;
        AgentRun run = new AgentRun(UUID.randomUUID(), createdAt);

        boolean transitioned = run.transitionTo(AgentRun.RunState.DONE);

        assertAll(
                () -> assertTrue(transitioned),
                () -> assertEquals(AgentRun.RunState.DONE, run.getState()),
                () -> assertTrue(run.getUpdatedAt().isAfter(createdAt))
        );
    }

    @ParameterizedTest
    @CsvSource({
            "PLANNING, true",
            "CODING, true",
            "REVIEWING, true",
            "TESTING, true",
            "FIXING, true",
            "IDLE, false",
            "PAUSED, false",
            "DONE, false",
            "FAILED, false",
            "PUBLISHING, false"
    })
    void isActiveIdentifiesActiveStates(AgentRun.RunState state, boolean expected) {
        assertEquals(expected, state.isActive());
    }

    @ParameterizedTest
    @CsvSource({
            "DONE, true",
            "FAILED, true",
            "IDLE, false",
            "PLANNING, false",
            "CODING, false",
            "REVIEWING, false",
            "TESTING, false",
            "FIXING, false",
            "PUBLISHING, false",
            "PAUSED, false"
    })
    void isTerminalIdentifiesTerminalStates(AgentRun.RunState state, boolean expected) {
        assertEquals(expected, state.isTerminal());
    }

    static Stream<Arguments> possibleTransitionsProvider() {
        return Stream.of(
                Arguments.of(AgentRun.RunState.IDLE, new AgentRun.RunState[]{AgentRun.RunState.PLANNING, AgentRun.RunState.FAILED}),
                Arguments.of(AgentRun.RunState.PLANNING, new AgentRun.RunState[]{AgentRun.RunState.CODING, AgentRun.RunState.FAILED, AgentRun.RunState.PAUSED}),
                Arguments.of(AgentRun.RunState.CODING, new AgentRun.RunState[]{AgentRun.RunState.REVIEWING, AgentRun.RunState.FAILED, AgentRun.RunState.PAUSED}),
                Arguments.of(AgentRun.RunState.REVIEWING, new AgentRun.RunState[]{AgentRun.RunState.TESTING, AgentRun.RunState.FIXING, AgentRun.RunState.FAILED, AgentRun.RunState.PAUSED}),
                Arguments.of(AgentRun.RunState.TESTING, new AgentRun.RunState[]{AgentRun.RunState.PUBLISHING, AgentRun.RunState.FIXING, AgentRun.RunState.PAUSED}),
                Arguments.of(AgentRun.RunState.FIXING, new AgentRun.RunState[]{AgentRun.RunState.REVIEWING, AgentRun.RunState.FAILED, AgentRun.RunState.PAUSED}),
                Arguments.of(AgentRun.RunState.PUBLISHING, new AgentRun.RunState[]{AgentRun.RunState.DONE, AgentRun.RunState.FAILED}),
                Arguments.of(AgentRun.RunState.PAUSED, new AgentRun.RunState[]{AgentRun.RunState.PLANNING, AgentRun.RunState.CODING, AgentRun.RunState.REVIEWING, AgentRun.RunState.TESTING, AgentRun.RunState.FIXING}),
                Arguments.of(AgentRun.RunState.DONE, new AgentRun.RunState[]{AgentRun.RunState.IDLE}),
                Arguments.of(AgentRun.RunState.FAILED, new AgentRun.RunState[]{AgentRun.RunState.IDLE})
        );
    }

    @ParameterizedTest
    @MethodSource("possibleTransitionsProvider")
    void getPossibleTransitionsReturnsCorrectStates(AgentRun.RunState state, AgentRun.RunState[] expected) {
        AgentRun.RunState[] transitions = state.getPossibleTransitions();

        assertAll(
                () -> assertEquals(expected.length, transitions.length),
                () -> assertArrayEquals(expected, transitions)
        );
    }

    @Test
    void canTransitionToIsConsistentWithGetPossibleTransitions() {
        for (AgentRun.RunState from : AgentRun.RunState.values()) {
            AgentRun.RunState[] possibleTransitions = from.getPossibleTransitions();

            for (AgentRun.RunState to : AgentRun.RunState.values()) {
                boolean shouldBeAllowed = java.util.Arrays.asList(possibleTransitions).contains(to);
                boolean isAllowed = from.canTransitionTo(to);

                assertEquals(shouldBeAllowed, isAllowed,
                        String.format("Inconsistency: %s -> %s. Expected: %s, Got: %s",
                                from, to, shouldBeAllowed, isAllowed));
            }
        }
    }

}
