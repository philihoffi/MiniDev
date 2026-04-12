package org.philipp.fun.minidev.web.controller;

import org.philipp.fun.minidev.core.AgentService;
import org.philipp.fun.minidev.run.AgentRun;
import org.philipp.fun.minidev.run.GameMetadata;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

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

    @GetMapping("/games/{runId}")
    public ResponseEntity<AgentRun> getRun(@PathVariable UUID runId) {
        return agentService.getRun(runId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/games/{runId}/content", produces = "text/html")
    public ResponseEntity<String> getGameContent(@PathVariable UUID runId) {
        String content = agentService.getGameContent(runId);
        if (content != null) {
            return ResponseEntity.ok(content);
        }
        return ResponseEntity.notFound().build();
    }
}
