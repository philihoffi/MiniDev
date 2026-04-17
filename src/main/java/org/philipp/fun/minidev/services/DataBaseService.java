package org.philipp.fun.minidev.services;

import org.philipp.fun.minidev.spring.model.AgentRun;
import org.philipp.fun.minidev.spring.repository.SpringDataAgentRunRepository;
import org.springframework.stereotype.Service;

@Service
public class DataBaseService {

    private final SpringDataAgentRunRepository agentRunRepository;

    public DataBaseService(SpringDataAgentRunRepository agentRunRepository) {
        this.agentRunRepository = agentRunRepository;
    }

    public void addToRepository(AgentRun run) {
        agentRunRepository.save(run);
    }

}
