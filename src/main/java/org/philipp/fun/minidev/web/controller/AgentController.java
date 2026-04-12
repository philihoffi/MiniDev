package org.philipp.fun.minidev.web.controller;

import org.philipp.fun.minidev.core.AgentService;
import org.philipp.fun.minidev.run.AgentRun;
import org.philipp.fun.minidev.run.GameMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/run")
    public String startRun() {
        log.info("REST request to start a new run");
        String runId = agentService.startNewRun().toString();
        log.info("New run started with ID: {}", runId);
        return runId;
    }

    @PostMapping("/games/{runId}/resume")
    public void resumeRun(@PathVariable UUID runId) {
        log.info("REST request to resume run: {}", runId);
        agentService.resumeRun(runId);
    }

    @GetMapping("/games")
    public List<GameMetadata> getAllGames() {
        log.info("REST request to get all games");
        return agentService.getAllGames();
    }

    @GetMapping("/games/{runId}")
    public ResponseEntity<AgentRun> getRun(@PathVariable UUID runId) {
        log.info("REST request to get run: {}", runId);
        return agentService.getRun(runId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/games/{runId}/content", produces = "text/html")
    public ResponseEntity<String> getGameContent(@PathVariable UUID runId) {
        log.debug("REST request to get game content for run: {}", runId);
        String content = agentService.getGameContent(runId);
        if (content != null) {
            return ResponseEntity.ok(content);
        }
        log.warn("Game content not found for run: {}", runId);
        return ResponseEntity.notFound().build();
    }
}
