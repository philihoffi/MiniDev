package org.philipp.fun.minidev.llm.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

/**
 * Request record for LLM chat completions.
 *
 * @param messages   messages for the conversation
 * @param temperature sampling temperature
 * @param maxTokens  maximum tokens to generate
 * @param jsonSchema JSON schema for structured output
 * @param sessionId  session identifier
 * @param model      model identifier
 */
public record LlmRequest(
        /** Messages for the conversation. */
        @NotNull List<Message> messages,
        /** Sampling temperature. */
        Double temperature,
        /** Maximum tokens to generate. */
        Integer maxTokens,
        /** JSON schema for structured output. */
        JsonSchema jsonSchema,
        /** Session identifier. */
        String sessionId,
        /** Model identifier. */
        String model
) {
    /**
     * Creates a simple LlmRequest with only messages.
     *
     * @param messages the messages
     */
    public LlmRequest(List<Message> messages) {
        this(messages, null, null, null, null, null);
    }

    /**
     * Creates an LlmRequest with messages and a JSON schema.
     *
     * @param messages   the messages
     * @param jsonSchema the JSON schema
     */
    public LlmRequest(List<Message> messages, JsonSchema jsonSchema) {
        this(messages, null, null, jsonSchema, null, null);
    }

    /**
     * Creates an LlmRequest with messages and a model.
     *
     * @param messages the messages
     * @param model    the model identifier
     */
    public LlmRequest(List<Message> messages, String model) {
        this(messages, null, null, null, null, model);
    }

    /**
     * A single message in the conversation.
     *
     * @param role    message role (user, assistant, system)
     * @param content message content
     */
    public record Message(
            /** Message role (user, assistant, system). */
            @NotNull String role,
            /** Message content. */
            @NotNull String content
    ) {
        /**
         * Creates a user message.
         *
         * @param content the message content
         * @return the message
         */
        public static Message user(String content) {
            return new Message("user", content);
        }

        /**
         * Creates an assistant message.
         *
         * @param content the message content
         * @return the message
         */
        public static Message assistant(String content) {
            return new Message("assistant", content);
        }

        /**
         * Creates a system message.
         *
         * @param content the message content
         * @return the message
         */
        public static Message system(String content) {
            return new Message("system", content);
        }
    }
}