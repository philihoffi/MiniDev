package org.philipp.fun.minidev.llm;

import jakarta.validation.constraints.NotNull;

public record LlmResponse(
        @NotNull String content,
        String model,
        Integer tokensUsed,
        boolean success,
        String errorMessage
) {
    public static LlmResponse success(String content, String model, Integer tokensUsed) {
        return new LlmResponse(content, model, tokensUsed, true, null);
    }

    public static LlmResponse failure(String errorMessage) {
        return new LlmResponse("", null, null, false, errorMessage);
    }
}
