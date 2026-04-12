package org.philipp.fun.minidev.web.controller;

import org.philipp.fun.minidev.core.AgentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import org.philipp.fun.minidev.run.GameMetadata;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/run")
    public String startRun() {
        return agentService.startNewRun().toString();
    }

    @PostMapping("/games/{runId}/resume")
    public void resumeRun(@PathVariable UUID runId) {
        agentService.resumeRun(runId);
    }

    @GetMapping("/games")
    public List<GameMetadata> getAllGames() {
        return agentService.getAllGames();
    }
}
