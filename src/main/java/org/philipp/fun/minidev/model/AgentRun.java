package org.philipp.fun.minidev.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class AgentRun {

    @Id
    private UUID id;
    private String startTime;

    public AgentRun() {
    }

    public AgentRun(UUID id, String startTime) {
        this.id = id;
        this.startTime = startTime;
    }


    public static AgentRun create() {
        return new AgentRun(UUID.randomUUID(), LocalDateTime.now().toString());
    }

    public UUID getId() {
        return id;
    }

    public String getStartTime() {
        return startTime;
    }
}
