package org.philipp.fun.minidev.run;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record GameMetadata(
        UUID runId,
        String name,
        String concept,
        List<String> todos,
        Path files,
        Path htmlPath,
        Path readmePath
) {

    public GameMetadata{
        Objects.requireNonNull(runId, "runId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(concept, "concept must not be null");
        Objects.requireNonNull(todos, "todos must not be null");
        Objects.requireNonNull(files, "files must not be null");
        Objects.requireNonNull(htmlPath, "htmlPath must not be null");
        Objects.requireNonNull(readmePath, "readmePath must not be null");
    }

    public GameMetadata(UUID runId, String name, String concept, List<String> todos, Path files) {
        this(runId, name, concept, todos, files, files.resolve("index.html"), files.resolve("README.md"));
    }



    @Override
    public String toString() {
        return "GameMetadata{" +
                "name='" + name + '\'' +
                ", concept='" + concept + '\'' +
                ", todos=" + todos +
                ", files=" + files +
                ", htmlPath=" + htmlPath +
                ", readmePath=" + readmePath +
                '}';
    }
}
