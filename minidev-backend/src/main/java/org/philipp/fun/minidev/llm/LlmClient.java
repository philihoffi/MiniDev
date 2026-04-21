package org.philipp.fun.minidev.llm;

import org.philipp.fun.minidev.dto.llm.LlmRequest;
import org.philipp.fun.minidev.dto.llm.LlmResponse;

public interface LlmClient {

    LlmResponse chat(LlmRequest request);
}
