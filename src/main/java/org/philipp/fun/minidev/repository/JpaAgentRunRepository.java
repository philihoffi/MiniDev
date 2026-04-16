package org.philipp.fun.minidev.repository;

import org.philipp.fun.minidev.model.AgentRun;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Repository
@Primary
public class JpaAgentRunRepository implements AgentRunRepository {

    private final SpringDataAgentRunRepository repository;

    public JpaAgentRunRepository(SpringDataAgentRunRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(AgentRun run) throws IOException {
        repository.save(run);
    }

    @Override
    public Optional<AgentRun> findById(UUID id) throws IOException {
        return repository.findById(id);
    }
}
