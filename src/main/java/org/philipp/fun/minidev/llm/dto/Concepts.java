package org.philipp.fun.minidev.llm.dto;

import java.util.List;
import java.util.Map;

public record Concepts(List<String> contents) {
    public static Map<String, Object> schema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "contents", Map.of(
                    "type", "array",
                    "items", Map.of("type", "string")
                )
            ),
            "required", List.of("contents"),
            "additionalProperties", false
        );
    }
}
