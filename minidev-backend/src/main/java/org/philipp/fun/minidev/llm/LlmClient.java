package org.philipp.fun.minidev.llm;

import org.philipp.fun.minidev.llm.objects.LlmRequest;
import org.philipp.fun.minidev.llm.objects.LlmResponse;

public interface LlmClient {

    LlmResponse chat(LlmRequest request);
}
