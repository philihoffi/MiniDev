package org.philipp.fun.minidev.llm.dto;

import java.util.List;
import java.util.Map;

public record Evaluation(String justification, String chosenConcept, int originalityScore, int feasibilityScore, int funFactorScore) {
    public static Map<String, Object> schema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "justification", Map.of("type", "string"),
                "chosenConcept", Map.of("type", "string"),
                "originalityScore", Map.of("type", "integer"),
                "feasibilityScore", Map.of("type", "integer"),
                "funFactorScore", Map.of("type", "integer")
            ),
            "required", List.of("justification", "chosenConcept", "originalityScore", "feasibilityScore", "funFactorScore"),
            "additionalProperties", false
        );
    }
}
