package org.philipp.fun.minidev.llm.objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record OpenRouterRequest(
    List<Message> messages,
    String model,
    Double temperature,
    @JsonProperty("max_tokens") Integer maxTokens
) {
    public record Message(
        String role,
        String content
    ) {}
}
