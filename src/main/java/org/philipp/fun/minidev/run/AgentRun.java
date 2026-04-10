package org.philipp.fun.minidev.run;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class AgentRun {

    private final UUID runId;
    private final Instant createdAt;

    private RunState state;
    private Instant updatedAt;

    public AgentRun(UUID runId, RunState state, Instant createdAt, Instant updatedAt) {
        this.runId = Objects.requireNonNull(runId, "runId must not be null");
        this.state = Objects.requireNonNull(state, "state must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not be before createdAt");
        }
    }

    public AgentRun(UUID runId, Instant createdAt) {
        this(runId, RunState.IDLE, Objects.requireNonNull(createdAt, "createdAt must not be null"), createdAt);
    }

    public static AgentRun create() {
        return new AgentRun(UUID.randomUUID(), Instant.now());
    }

    public boolean transitionTo(RunState nextState, Instant changedAt) {
        Objects.requireNonNull(nextState, "nextState must not be null");
        Objects.requireNonNull(changedAt, "changedAt must not be null");
        if (changedAt.isBefore(updatedAt)) {
            throw new IllegalArgumentException("changedAt must not be before updatedAt");
        }
        this.state = nextState;
        this.updatedAt = changedAt;
        return true;
    }

    public boolean transitionTo(RunState nextState) {
        return transitionTo(nextState, Instant.now());
    }

    public UUID getRunId() {
        return runId;
    }

    public RunState getState() {
        return state;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "AgentRun{"
                + "runId='" + runId + '\''
                + ", state=" + state
                + ", createdAt=" + createdAt
                + ", updatedAt=" + updatedAt
                + '}';
    }


    public enum RunState {
        IDLE,
        PLANNING,
        CODING,
        REVIEWING,
        TESTING,
        FIXING,
        PUBLISHING,
        DONE,
        FAILED,
        PAUSED
    }
}
