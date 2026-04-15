package org.philipp.fun.minidev.llm.dto;

import java.util.List;
import java.util.Map;

public record Design(String content) {
    public static Map<String, Object> schema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "content", Map.of("type", "string")
                ),
                "required", List.of("content"),
                "additionalProperties", false
        );
    }
}
