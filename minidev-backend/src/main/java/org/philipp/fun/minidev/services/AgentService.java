package org.philipp.fun.minidev.services;

import org.philipp.fun.minidev.model.AgentRun;
import org.philipp.fun.minidev.repository.SpringDataAgentRunRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class AgentService {

    private final SpringDataAgentRunRepository agentRunRepository;

    public AgentService(SpringDataAgentRunRepository agentRunRepository) {
        this.agentRunRepository = agentRunRepository;
    }

    public AgentRun startRun() throws IOException {
        AgentRun run = new AgentRun();
        agentRunRepository.save(run);
        return run;
    }
}
