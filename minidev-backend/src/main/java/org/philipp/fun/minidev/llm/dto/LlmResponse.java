package org.philipp.fun.minidev.llm.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.constraints.NotNull;

/**
 * Record representing a response from an LLM.
 *
 * @param content      the response content
 * @param model        the model used
 * @param tokensUsed   the number of tokens consumed
 * @param success      whether the request succeeded
 * @param errorMessage an error message if the request failed
 */
public record LlmResponse(
        @NotNull String content,
        String model,
        Integer tokensUsed,
        boolean success,
        String errorMessage
) {

    /** Shared ObjectMapper for deserialization. */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Deserializes the content into the given type.
     *
     * @param <T>   the target type
     * @param clazz the target class
     * @return the deserialized object
     */
    public <T> T getContentAs(Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(content, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "Failed to deserialize response content: " + content, e);
        }
    }

    /**
     * Creates a success response.
     *
     * @param content    the response content
     * @param model      the model used
     * @param tokensUsed the number of tokens consumed
     * @return a success LlmResponse
     */
    public static LlmResponse success(String content, String model, Integer tokensUsed) {
        return new LlmResponse(content, model, tokensUsed, true, null);
    }

    /**
     * Creates a failure response.
     *
     * @param errorMessage the error message
     * @return a failure LlmResponse
     */
    public static LlmResponse failure(String errorMessage) {
        return new LlmResponse("", null, null, false, errorMessage);
    }
}
