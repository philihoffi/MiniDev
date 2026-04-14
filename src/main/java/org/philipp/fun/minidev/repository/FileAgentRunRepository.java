package org.philipp.fun.minidev.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.philipp.fun.minidev.model.AgentRun;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Repository
public class FileAgentRunRepository implements AgentRunRepository {

    private final ObjectMapper objectMapper;

    public FileAgentRunRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(AgentRun run) throws IOException {
        Path runDir = Paths.get("generated-games", "run-" + run.id());
        Files.createDirectories(runDir);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(runDir.resolve("metadata.json").toFile(), run);
    }

    @Override
    public Optional<AgentRun> findById(String id) throws IOException {
        Path metadataPath = Paths.get("generated-games", "run-" + id, "metadata.json");
        if (!Files.exists(metadataPath)) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.readValue(metadataPath.toFile(), AgentRun.class));
    }
}
