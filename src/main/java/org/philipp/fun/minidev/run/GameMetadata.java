package org.philipp.fun.minidev.run;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record GameMetadata(
        String name,
        String concept,
        List<String> todos,
        Path files,
        Path htmlPath,
        Path readmePath
) {

    public GameMetadata{
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(concept, "concept must not be null");
        Objects.requireNonNull(todos, "todos must not be null");
        Objects.requireNonNull(files, "files must not be null");
        Objects.requireNonNull(htmlPath, "htmlPath must not be null");
        Objects.requireNonNull(readmePath, "readmePath must not be null");
    }

    public GameMetadata(String name, String concept, List<String> todos, Path files) {
        Path readmePath = files.resolve("README.md");
        Path htmlPath = files.resolve("index.html");
        this(name, concept, todos, files, htmlPath, readmePath);
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
