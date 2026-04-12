package org.philipp.fun.minidev.web.controller;

import org.philipp.fun.minidev.core.AgentService;
import org.philipp.fun.minidev.core.GameStorageService;
import org.philipp.fun.minidev.run.AgentRun;
import org.philipp.fun.minidev.run.GameMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final AgentService agentService;
    private final GameStorageService gameStorageService;

    public AgentController(AgentService agentService, GameStorageService gameStorageService) {
        this.agentService = agentService;
        this.gameStorageService = gameStorageService;
    }

    @PostMapping("/run")
    public ResponseEntity<String> startRun() {
        log.info("REST request to start a new run");
        try {
            String runId = agentService.startNewRun().toString();
            log.info("New run started with ID: {}", runId);
            return ResponseEntity.ok(runId);
        } catch (IllegalStateException e) {
            log.warn("Cannot start run: {}", e.getMessage());
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    @PostMapping("/games/{runId}/resume")
    public ResponseEntity<Void> resumeRun(@PathVariable UUID runId) {
        log.info("REST request to resume run: {}", runId);
        try {
            agentService.resumeRun(runId);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            log.warn("Cannot resume run {}: {}", runId, e.getMessage());
            return ResponseEntity.status(409).build();
        }
    }

    @GetMapping("/games")
    public List<GameMetadata> getAllGames() {
        log.info("REST request to get all games");
        return gameStorageService.getAllGames();
    }

    @GetMapping("/games/{runId}")
    public ResponseEntity<AgentRun> getRun(@PathVariable UUID runId) {
        log.info("REST request to get run: {}", runId);
        return agentService.getRun(runId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/games/{runId}/components")
    public ResponseEntity<Map<String, String>> getGameComponents(@PathVariable UUID runId) {
        log.debug("REST request to get game components for run: {}", runId);
        Map<String, String> components = gameStorageService.getGameComponentContent(runId);
        if (components != null) {
            return ResponseEntity.ok(components);
        }
        log.warn("Game components not found for run: {}", runId);
        return ResponseEntity.notFound().build();
    }
}
