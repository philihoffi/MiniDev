package org.philipp.fun.minidev.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.philipp.fun.minidev.model.AgentRun;
import org.philipp.fun.minidev.repository.FileAgentRunRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentServiceTest {

    @Test
    void testStartRunSavesMetadata() throws IOException {
        AgentService agentService = new AgentService(new FileAgentRunRepository(new ObjectMapper()));
        AgentRun run = agentService.startRun();
        
        Path metadataPath = Paths.get("generated-games", "run-" + run.id(), "metadata.json");
        assertTrue(Files.exists(metadataPath));
        
        // Clean up
        Files.deleteIfExists(metadataPath);
        Files.deleteIfExists(Paths.get("generated-games", "run-" + run.id()));
    }

    @Test
    void testLoadExistingRun() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        FileAgentRunRepository repository = new FileAgentRunRepository(objectMapper);
        AgentService agentService = new AgentService(repository);
        
        AgentRun originalRun = agentService.startRun();
        
        Optional<AgentRun> loadedRun = repository.findById(originalRun.id());
        
        assertTrue(loadedRun.isPresent());
        assertEquals(originalRun.id(), loadedRun.get().id());
        
        // Clean up
        Path metadataPath = Paths.get("generated-games", "run-" + originalRun.id(), "metadata.json");
        Files.deleteIfExists(metadataPath);
        Files.deleteIfExists(Paths.get("generated-games", "run-" + originalRun.id()));
    }
}
