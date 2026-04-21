package org.philipp.fun.minidev.llm;

import org.philipp.fun.minidev.dto.llm.LlmModel;
import org.philipp.fun.minidev.dto.llm.LlmRequest;
import org.philipp.fun.minidev.dto.llm.LlmResponse;

import java.util.List;

public interface LlmClient {

    LlmResponse chat(LlmRequest request);

    List<LlmModel> getModels();

    default List<LlmModel> getModels(String category, String supportedParameters, String outputModalities) {
        return getModels();
    }
}
