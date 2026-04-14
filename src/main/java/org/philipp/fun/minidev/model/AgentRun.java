package org.philipp.fun.minidev.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record AgentRun(String id, String startTime, Status status) {

    public enum Status {
        STARTED, RUNNING, COMPLETED, FAILED
    }

    public static AgentRun create() {
        return new AgentRun(UUID.randomUUID().toString(), LocalDateTime.now().toString(), Status.STARTED);
    }
}
