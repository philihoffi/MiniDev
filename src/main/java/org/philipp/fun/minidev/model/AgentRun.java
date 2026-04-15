package org.philipp.fun.minidev.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class AgentRun {

    private UUID id;
    private String startTime;
    private Status status;

    public AgentRun() {
    }

    public AgentRun(UUID id, String startTime, Status status) {
        this.id = id;
        this.startTime = startTime;
        this.status = status;
    }

    public enum Status {
        STARTED, RUNNING, COMPLETED, FAILED
    }

    public static AgentRun create() {
        return new AgentRun(UUID.randomUUID(), LocalDateTime.now().toString(), Status.STARTED);
    }

    public UUID getId() {
        return id;
    }

    public String getStartTime() {
        return startTime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
