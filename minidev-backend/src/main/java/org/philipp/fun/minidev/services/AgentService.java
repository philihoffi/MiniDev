package org.philipp.fun.minidev.services;

import org.philipp.fun.minidev.spring.model.AgentRun;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class AgentService {

    private final DataBaseService dataSource;

    public AgentService(DataBaseService dataSource) {
        this.dataSource = dataSource;
    }

    public AgentRun startRun() throws IOException {
        AgentRun run = new AgentRun();
        dataSource.addToRepository(run);
        return run;
    }
}
