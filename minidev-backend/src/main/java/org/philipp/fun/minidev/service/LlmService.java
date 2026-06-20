package org.philipp.fun.minidev.service;

import java.util.List;

import org.philipp.fun.minidev.dto.llm.LlmModel;
import org.philipp.fun.minidev.llm.LlmClient;
import org.springframework.stereotype.Service;

/**
 * Service layer for LLM-related operations.
 */
@Service
public class LlmService {

    /** The underlying LLM client. */
    private final LlmClient llmClient;

    /**
     * Constructs an LlmService with the given client.
     *
     * @param llmClient the LLM client
     */
    public LlmService(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    /**
     * Returns all available models.
     *
     * @return list of models
     */
    public List<LlmModel> getModels() {
        return llmClient.getModels();
    }

    /**
     * Returns models filtered by optional criteria.
     *
     * @param category            optional category filter
     * @param supportedParameters optional supported parameters filter
     * @param outputModalities    optional output modalities filter
     * @return list of matching models
     */
    public List<LlmModel> getModels(
            String category,
            String supportedParameters,
            String outputModalities) {
        return llmClient.getModels(category, supportedParameters, outputModalities);
    }
}
