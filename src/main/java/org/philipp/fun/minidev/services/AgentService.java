package org.philipp.fun.minidev.services;

import org.philipp.fun.minidev.model.AgentRun;
import org.philipp.fun.minidev.repository.AgentRunRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class AgentService {

    private final AgentRunRepository repository;

    public AgentService(AgentRunRepository repository) {
        this.repository = repository;
    }

    public AgentRun startRun() throws IOException {
        AgentRun run = AgentRun.create();
        repository.save(run);
        return run;
    }
}
