package org.philipp.fun.minidev.service;

import org.philipp.fun.minidev.dto.llm.LlmModel;
import org.philipp.fun.minidev.llm.LlmClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LlmService {

    private final LlmClient llmClient;

    public LlmService(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public List<LlmModel> getModels() {
        return llmClient.getModels();
    }

    public List<LlmModel> getModels(String category, String supportedParameters, String outputModalities) {
        return llmClient.getModels(category, supportedParameters, outputModalities);
    }
}
