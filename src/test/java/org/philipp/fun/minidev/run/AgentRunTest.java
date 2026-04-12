package org.philipp.fun.minidev.run;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;


class AgentRunTest {

    @Test
    void constructorStoresAllProvidedValues() {
        UUID runId = UUID.randomUUID();
        Instant createdAt = Instant.EPOCH;
        Instant updatedAt = createdAt.plusSeconds(30);
        GameMetadata metadata = new GameMetadata(runId, "Test Game", "Concept", List.of("Task 1"), Path.of("test-path"));

        AgentRun run = new AgentRun(AgentRun.RunState.PLANNING, createdAt, updatedAt, metadata);

        assertAll(
                () -> assertEquals(runId, run.getGameMetadata().runId()),
                () -> assertEquals(AgentRun.RunState.PLANNING, run.getState()),
                () -> assertEquals(createdAt, run.getCreatedAt()),
                () -> assertEquals(updatedAt, run.getUpdatedAt()),
                () -> assertEquals(metadata, run.getGameMetadata())
        );
    }

    @Test
    void convenienceConstructorInitializesIdleStateAndUsesCreatedAtAsUpdatedAt() {
        String storagePath = "test-storage";
        AgentRun run = new AgentRun(storagePath);

        assertAll(
                () -> assertNotNull(run.getGameMetadata().runId()),
                () -> assertEquals(AgentRun.RunState.IDLE, run.getState()),
                () -> assertNotNull(run.getCreatedAt()),
                () -> assertEquals(run.getCreatedAt(), run.getUpdatedAt()),
                () -> assertNotNull(run.getGameMetadata()),
                () -> assertEquals("Untitled Game", run.getGameMetadata().name()),
                () -> assertTrue(run.getGameMetadata().files().toString().contains(storagePath))
        );
    }

    @Test
    void createBuildsIdleRunWithGeneratedIdAndMatchingTimestamps() {
        AgentRun run = new AgentRun("test-storage");

        assertAll(
                () -> assertNotNull(run.getGameMetadata().runId()),
                () -> assertEquals(AgentRun.RunState.IDLE, run.getState()),
                () -> assertNotNull(run.getCreatedAt()),
                () -> assertEquals(run.getCreatedAt(), run.getUpdatedAt()),
                () -> assertNotNull(run.getGameMetadata())
        );
    }

    @Test
    void fullConstructorRejectsNullArguments() {
        UUID runId = UUID.randomUUID();
        Instant timestamp = Instant.EPOCH;
        GameMetadata metadata = new GameMetadata(runId, "Name", "Concept", List.of(), Path.of("test"));

        assertAll(
                () -> assertThrows(NullPointerException.class, () -> new AgentRun(AgentRun.RunState.IDLE, timestamp, timestamp, null)),
                () -> assertThrows(NullPointerException.class, () -> new AgentRun(null, timestamp, timestamp, metadata)),
                () -> assertThrows(NullPointerException.class, () -> new AgentRun(AgentRun.RunState.IDLE, null, timestamp, metadata)),
                () -> assertThrows(NullPointerException.class, () -> new AgentRun(AgentRun.RunState.IDLE, timestamp, null, metadata))
        );
    }

    @Test
    void constructorRejectsUpdatedAtBeforeCreatedAt() {
        UUID runId = UUID.randomUUID();
        Instant createdAt = Instant.EPOCH;
        Instant updatedAt = createdAt.minusSeconds(1);
        GameMetadata metadata = new GameMetadata(runId, "Name", "Concept", List.of(), Path.of("test"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new AgentRun(AgentRun.RunState.IDLE, createdAt, updatedAt, metadata)
        );

        assertEquals("updatedAt must not be before createdAt", exception.getMessage());
    }

    @Test
    void storagePathConstructorRejectsNull() {
        assertThrows(NullPointerException.class, () -> new AgentRun((String) null));
    }

    @Test
    void transitionToUpdatesStateAndUpdatedAt() {
        AgentRun run = new AgentRun("test-storage");
        Instant createdAt = run.getCreatedAt();
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
        AgentRun run = new AgentRun("test-storage");
        Instant createdAt = run.getCreatedAt();

        assertAll(
                () -> assertThrows(NullPointerException.class, () -> run.transitionTo(null, createdAt.plusSeconds(1))),
                () -> assertThrows(NullPointerException.class, () -> run.transitionTo(AgentRun.RunState.CODING, null))
        );
    }

    @Test
    void transitionToRejectsTimestampBeforeCurrentUpdatedAt() {
        Instant createdAt = Instant.EPOCH;
        UUID runId = UUID.randomUUID();
        GameMetadata metadata = new GameMetadata(runId, "Name", "Concept", List.of(), Path.of("test"));
        AgentRun run = new AgentRun(AgentRun.RunState.PLANNING, createdAt, createdAt.plusSeconds(10), metadata);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> run.transitionTo(AgentRun.RunState.CODING, createdAt.plusSeconds(9))
        );

        assertEquals("changedAt must not be before updatedAt", exception.getMessage());
    }

    @Test
    void transitionToWithoutTimestampUsesCurrentTime() {
        AgentRun run = new AgentRun("test-storage");
        Instant initialUpdatedAt = run.getUpdatedAt();

        boolean transitioned = run.transitionTo(AgentRun.RunState.DONE);

        assertAll(
                () -> assertTrue(transitioned),
                () -> assertEquals(AgentRun.RunState.DONE, run.getState()),
                () -> assertTrue(!run.getUpdatedAt().isBefore(initialUpdatedAt))
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
