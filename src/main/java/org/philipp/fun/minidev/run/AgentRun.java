package org.philipp.fun.minidev.run;

import java.time.Instant;
import java.util.Arrays;
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

    public AgentRun() {
        this(UUID.randomUUID(), Instant.now());
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
        PAUSED;

        public boolean canTransitionTo(RunState target) {
            return Arrays.asList(getPossibleTransitions()).contains(target);
        }

        public boolean isActive() {
            return this == PLANNING || this == CODING || this == REVIEWING
                || this == TESTING || this == FIXING;
        }

        public boolean isTerminal() {
            return this == DONE || this == FAILED;
        }

        public RunState[] getPossibleTransitions() {
            return switch (this) {
                case IDLE -> new RunState[]{PLANNING, FAILED};
                case PLANNING -> new RunState[]{CODING, FAILED, PAUSED};
                case CODING, FIXING -> new RunState[]{REVIEWING, FAILED, PAUSED};
                case REVIEWING -> new RunState[]{TESTING, FIXING, FAILED, PAUSED};
                case TESTING -> new RunState[]{PUBLISHING, FIXING, PAUSED};
                case PUBLISHING -> new RunState[]{DONE, FAILED};
                case PAUSED -> new RunState[]{PLANNING, CODING, REVIEWING, TESTING, FIXING};
                case DONE, FAILED -> new RunState[]{IDLE};
            };
        }
    }
}
