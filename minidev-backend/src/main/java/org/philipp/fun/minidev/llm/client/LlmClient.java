package org.philipp.fun.minidev.llm.client;

import java.util.List;

import org.philipp.fun.minidev.llm.dto.LlmModel;
import org.philipp.fun.minidev.llm.dto.LlmRequest;
import org.philipp.fun.minidev.llm.dto.LlmResponse;

/**
 * Client interface for interacting with an LLM provider.
 */
public interface LlmClient {

    /**
     * Sends a chat request and returns the response.
     *
     * @param request the chat request
     * @return the LLM response
     */
    LlmResponse chat(LlmRequest request);

    /**
     * Returns all available models.
     *
     * @return list of models
     */
    List<LlmModel> getModels();

    /**
     * Returns models filtered by optional criteria. The default implementation
     * ignores filters and returns all models.
     *
     * @param category            optional category filter
     * @param supportedParameters optional supported parameters filter
     * @param outputModalities    optional output modalities filter
     * @return list of matching models
     */
    default List<LlmModel> getModels(
            String category,
            String supportedParameters,
            String outputModalities) {
        return getModels();
    }
}
