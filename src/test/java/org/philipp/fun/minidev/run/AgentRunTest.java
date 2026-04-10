package org.philipp.fun.minidev.run;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


class AgentRunTest {

    @Test
    void constructorStoresAllProvidedValues() {
        UUID runId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-10T10:15:30Z");
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
        Instant createdAt = Instant.parse("2026-04-10T10:15:30Z");

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
        AgentRun run = AgentRun.create();

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
        Instant timestamp = Instant.parse("2026-04-10T10:15:30Z");

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
        Instant createdAt = Instant.parse("2026-04-10T10:15:30Z");
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
        Instant createdAt = Instant.parse("2026-04-10T10:15:30Z");
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
        Instant createdAt = Instant.parse("2026-04-10T10:15:30Z");
        AgentRun run = new AgentRun(UUID.randomUUID(), createdAt);

        assertAll(
                () -> assertThrows(NullPointerException.class, () -> run.transitionTo(null, createdAt.plusSeconds(1))),
                () -> assertThrows(NullPointerException.class, () -> run.transitionTo(AgentRun.RunState.CODING, null))
        );
    }

    @Test
    void transitionToRejectsTimestampBeforeCurrentUpdatedAt() {
        Instant createdAt = Instant.parse("2026-04-10T10:15:30Z");
        AgentRun run = new AgentRun(UUID.randomUUID(), AgentRun.RunState.PLANNING, createdAt, createdAt.plusSeconds(10));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> run.transitionTo(AgentRun.RunState.CODING, createdAt.plusSeconds(9))
        );

        assertEquals("changedAt must not be before updatedAt", exception.getMessage());
    }

    @Test
    void transitionToWithoutTimestampUsesCurrentTime() {
        Instant createdAt = Instant.parse("2026-04-10T10:15:30Z");
        AgentRun run = new AgentRun(UUID.randomUUID(), createdAt);

        boolean transitioned = run.transitionTo(AgentRun.RunState.DONE);

        assertAll(
                () -> assertTrue(transitioned),
                () -> assertEquals(AgentRun.RunState.DONE, run.getState()),
                () -> assertFalse(run.getUpdatedAt().isBefore(createdAt))
        );
    }

}
