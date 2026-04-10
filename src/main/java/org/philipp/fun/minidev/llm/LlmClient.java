package org.philipp.fun.minidev.llm;

public interface LlmClient {

    LlmResponse chat(LlmRequest request);
}
