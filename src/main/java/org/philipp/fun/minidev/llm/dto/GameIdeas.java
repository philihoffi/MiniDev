package org.philipp.fun.minidev.llm.dto;

import java.util.List;
import java.util.Map;

public record GameIdeas(List<GameIdea> ideas) {
    public static Map<String, Object> schema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "ideas", Map.of(
                    "type", "array",
                    "items", GameIdea.schema()
                )
            ),
            "required", List.of("ideas"),
            "additionalProperties", false
        );
    }
}
