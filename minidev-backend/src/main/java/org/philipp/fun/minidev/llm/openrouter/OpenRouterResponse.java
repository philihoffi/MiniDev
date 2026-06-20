package org.philipp.fun.minidev.llm.openrouter;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from the OpenRouter chat completions endpoint.
 *
 * @param id       the response identifier
 * @param model    the model used
 * @param provider the provider used
 * @param choices  the list of choices
 * @param usage    token usage information
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenRouterResponse(
        /** The response identifier. */
        String id,
        /** The model used. */
        String model,
        /** The provider used. */
        String provider,
        /** The list of choices. */
        List<Choice> choices,
        /** Token usage information. */
        Usage usage
) {

    /**
     * A single choice from the response.
     *
     * @param message      the response message
     * @param finishReason the finish reason
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(
            /** The response message. */
            Message message,
            /** The finish reason. */
            @JsonProperty("finish_reason") String finishReason
    ) {
    }

    /**
     * A message in the chat conversation.
     *
     * @param role      the message role
     * @param content   the message content
     * @param refusal   refusal content if applicable
     * @param reasoning reasoning content if applicable
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(
            /** The message role. */
            String role,
            /** The message content. */
            String content,
            /** Refusal content if applicable. */
            String refusal,
            /** Reasoning content if applicable. */
            String reasoning
    ) {
    }

    /**
     * Token usage statistics.
     *
     * @param promptTokens     number of prompt tokens
     * @param completionTokens number of completion tokens
     * @param totalTokens      total number of tokens
     * @param cost             the cost of the request
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(
            /** Number of prompt tokens. */
            @JsonProperty("prompt_tokens") Integer promptTokens,
            /** Number of completion tokens. */
            @JsonProperty("completion_tokens") Integer completionTokens,
            /** Total number of tokens. */
            @JsonProperty("total_tokens") Integer totalTokens,
            /** The cost of the request. */
            Double cost
    ) {
    }
}