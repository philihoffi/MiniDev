package org.philipp.fun.minidev.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.philipp.fun.minidev.run.AgentRun;
import org.philipp.fun.minidev.run.GameMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class GameStorageService {

    private static final Logger log = LoggerFactory.getLogger(GameStorageService.class);

    private final ObjectMapper objectMapper;
    private final String storageBasePath;

    public GameStorageService(ObjectMapper objectMapper, 
                              @Value("${app.storage.base-path:generated-games}") String storageBasePath) {
        this.objectMapper = objectMapper;
        this.storageBasePath = storageBasePath;
    }

    public void saveMetadata(AgentRun run) {
        GameMetadata metadata = run.getGameMetadata();
        if (metadata == null) return;
        Path files = metadata.files();
        if (files == null) return;
        
        Path metadataPath = files.resolve("metadata.json");
        try {
            Files.createDirectories(files);
            this.objectMapper.writeValue(metadataPath.toFile(), metadata);
            log.info("Saved metadata for run {} to {}", run.getGameMetadata().runId(), metadataPath);
        } catch (IOException e) {
            log.error("Failed to save metadata for run {}", run.getGameMetadata().runId(), e);
        }
    }

    public List<GameMetadata> getAllGames() {
        Path basePath = Paths.get(storageBasePath);
        if (!Files.exists(basePath) || !Files.isDirectory(basePath)) {
            return List.of();
        }

        try (Stream<Path> walk = Files.walk(basePath, 1)) {
            return walk
                    .filter(Files::isDirectory)
                    .filter(path -> !path.equals(basePath))
                    .map(path -> path.resolve("metadata.json"))
                    .filter(Files::exists)
                    .map(metadataPath -> {
                        try {
                            GameMetadata metadata = objectMapper.readValue(metadataPath.toFile(), GameMetadata.class);
                            if (metadata.runId() == null) {
                                String dirName = metadataPath.getParent().getFileName().toString();
                                if (dirName.startsWith("run-")) {
                                    try {
                                        UUID runId = UUID.fromString(dirName.substring(4));
                                        metadata = new GameMetadata(runId, metadata.name(), metadata.concept(), metadata.coreMechanic(), metadata.todos(), metadata.doneTodos(), metadata.phaseHistory(), metadata.files());
                                    } catch (IllegalArgumentException ignored) {}
                                }
                            }
                            return metadata;
                        } catch (IOException e) {
                            log.error("Failed to load metadata from {}", metadataPath, e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException e) {
            log.error("Failed to list games in {}", storageBasePath, e);
            return List.of();
        }
    }

    public Map<String, String> getGameComponentContent(UUID runId) {
        String content = getGameContent(runId);
        if (content == null) {
            return null;
        }

        String html = content;
        String css = "";
        String js = "";

        if (content.contains("<style>")) {
            int start = content.indexOf("<style>");
            int end = content.indexOf("</style>");
            if (end > start) {
                css = content.substring(start + 7, end);
                html = html.replace(content.substring(start, end + 8), "<!-- CSS removed to editor -->");
            }
        }
        if (content.contains("<script>")) {
            int start = content.indexOf("<script>");
            int end = content.lastIndexOf("</script>");
            if (end > start) {
                js = content.substring(start + 8, end);
                html = html.replace(content.substring(start, end + 9), "<!-- JS removed to editor -->");
            }
        }

        return Map.of(
                "html", html,
                "css", css,
                "js", js
        );
    }

    public String getGameContent(UUID runId) {
        Path runPath = Paths.get(storageBasePath, "run-" + runId, "index.html");
        if (Files.exists(runPath)) {
            try {
                return Files.readString(runPath);
            } catch (IOException e) {
                log.error("Failed to read game content: {}", runPath, e);
            }
        }
        return null;
    }

    public AgentRun loadRunFromDisk(UUID runId) {
        Path runPath = Paths.get(storageBasePath, "run-" + runId);
        Path metadataPath = runPath.resolve("metadata.json");

        if (Files.exists(metadataPath)) {
            try {
                GameMetadata metadata = objectMapper.readValue(metadataPath.toFile(), GameMetadata.class);
                return new AgentRun(AgentRun.RunState.PAUSED, Instant.now(), Instant.now(), metadata);
            } catch (IOException e) {
                log.error("Failed to load metadata for run {}", runId, e);
            }
        }
        return null;
    }
    
    public String getStorageBasePath() {
        return storageBasePath;
    }

    public void writeGameContent(UUID runId, String content) throws IOException {
        Path runPath = Paths.get(storageBasePath, "run-" + runId);
        Files.createDirectories(runPath);
        Path indexPath = runPath.resolve("index.html");
        Files.writeString(indexPath, content);
    }
}
