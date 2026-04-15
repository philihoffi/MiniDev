package org.philipp.fun.minidev.llm.dto;

import java.util.List;
import java.util.Map;

public record GameTheme(String theme) {
    public static Map<String, Object> schema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "theme", Map.of(
                    "type", "string",
                    "pattern", "^[a-z]+( [a-z]+){4}$"
                )
            ),
            "required", List.of("theme"),
            "additionalProperties", false
        );
    }
}
