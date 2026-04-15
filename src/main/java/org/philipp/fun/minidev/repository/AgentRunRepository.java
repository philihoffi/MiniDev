package org.philipp.fun.minidev.repository;

import org.philipp.fun.minidev.model.AgentRun;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public interface AgentRunRepository {
    void save(AgentRun run) throws IOException;
    Optional<AgentRun> findById(UUID id) throws IOException;
}
