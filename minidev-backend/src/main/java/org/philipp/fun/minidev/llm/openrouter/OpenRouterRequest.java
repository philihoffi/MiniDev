package org.philipp.fun.minidev.llm.openrouter;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.philipp.fun.minidev.dto.llm.JsonSchema;

/**
 * Request payload for the OpenRouter chat completions API.
 *
 * @param messages       the list of chat messages
 * @param model          the model identifier
 * @param temperature    the temperature for sampling
 * @param maxTokens      the maximum number of tokens to generate
 * @param responseFormat the response format (e.g. JSON)
 * @param sessionId      optional session identifier
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenRouterRequest(
        /** The list of chat messages. */
        List<Message> messages,
        /** The model identifier. */
        String model,
        /** The temperature for sampling. */
        Double temperature,
        /** The maximum number of tokens to generate. */
        @JsonProperty("max_tokens") Integer maxTokens,
        /** The response format (e.g. JSON). */
        @JsonProperty("response_format") ResponseFormat responseFormat,
        /** Optional session identifier. */
        @JsonProperty("session_id") String sessionId
) {

    /**
     * A chat message with a role and content.
     *
     * @param role    the message role (system, user, assistant)
     * @param content the message content
     */
    public record Message(
            /** The message role (system, user, assistant). */
            String role,
            /** The message content. */
            String content
    ) {
    }

    /**
     * Response format specification for structured output.
     *
     * @param type       the format type (e.g. json_object, json_schema)
     * @param jsonSchema the JSON schema for structured output
     */
    public record ResponseFormat(
            /** The format type (e.g. json_object, json_schema). */
            String type,
            /** The JSON schema for structured output. */
            @JsonProperty("json_schema") JsonSchema jsonSchema
    ) {
        /** Pre-built JSON object response format. */
        public static ResponseFormat JSON =
                new ResponseFormat("json_object", null);
    }
}