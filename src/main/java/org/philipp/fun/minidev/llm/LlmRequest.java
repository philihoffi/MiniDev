package org.philipp.fun.minidev.llm;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record LlmRequest(
        @NotNull List<Message> messages,
        Double temperature,
        Integer maxTokens,
        boolean jsonMode,
        Object jsonSchema,
        String sessionId
) {
    public LlmRequest(List<Message> messages) {
        this(messages, null, null, false, null, null);
    }

    public LlmRequest(List<Message> messages, boolean jsonMode) {
        this(messages, null, null, jsonMode, null, null);
    }

    public LlmRequest(List<Message> messages, Object jsonSchema) {
        this(messages, null, null, true, jsonSchema, null);
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
