package org.philipp.fun.minidev.llm.objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;

public record LlmResponse(
        @NotNull String content,
        String model,
        Integer tokensUsed,
        boolean success,
        String errorMessage
) {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public <T> T getContentAs(Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(content, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize response content: " + content, e);
        }
    }

    public static LlmResponse success(String content, String model, Integer tokensUsed) {
        return new LlmResponse(content, model, tokensUsed, true, null);
    }

    public static LlmResponse failure(String errorMessage) {
        return new LlmResponse("", null, null, false, errorMessage);
    }
}
