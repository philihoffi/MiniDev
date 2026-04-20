package org.philipp.fun.minidev.services;

import org.philipp.fun.minidev.spring.model.AgentRun;
import org.philipp.fun.minidev.spring.repository.SpringDataAgentRunRepository;
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
