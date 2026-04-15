package org.philipp.fun.minidev.llm.dto;

import java.util.List;
import java.util.Map;

public record GameIdea(
    String name,
    String hook,
    String coreMechanic,
    String uniqueness,
    String similarityRisk,
    int feasibility,
    int originalityScore
) {
    public static Map<String, Object> schema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "name", Map.of("type", "string"),
                "hook", Map.of("type", "string"),
                "coreMechanic", Map.of("type", "string"),
                "uniqueness", Map.of("type", "string"),
                "similarityRisk", Map.of("type", "string"),
                "feasibility", Map.of("type", "integer"),
                "originalityScore", Map.of("type", "integer")
            ),
            "required", List.of("name", "hook", "coreMechanic", "uniqueness", "similarityRisk", "feasibility", "originalityScore"),
            "additionalProperties", false
        );
    }
}
