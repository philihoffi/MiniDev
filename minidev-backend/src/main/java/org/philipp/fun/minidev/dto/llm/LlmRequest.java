package org.philipp.fun.minidev.dto.llm;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record LlmRequest(
        @NotNull List<Message> messages,
        Double temperature,
        Integer maxTokens,
        JsonSchema jsonSchema,
        String sessionId,
        String model
) {
    public LlmRequest(List<Message> messages) {
        this(messages, null, null, null, null, null);
    }

    public LlmRequest(List<Message> messages, JsonSchema jsonSchema) {
        this(messages, null, null, jsonSchema, null, null);
    }

    public LlmRequest(List<Message> messages, String model) {
        this(messages, null, null, null, null, model);
    }

    public record Message(
            @NotNull String role,
            @NotNull String content
    ) {
        public static Message user(String content) {
            return new Message("user", content);
        }

        public static Message assistant(String content) {
            return new Message("assistant", content);
        }

        public static Message system(String content) {
            return new Message("system", content);
        }
    }
}
