package org.philipp.fun.minidev.dto.llm;

import java.util.List;

public record LlmModel(
    String id,
    String name,
    String description,
    Integer contextLength,
    Long created,
    Architecture architecture,
    Pricing pricing,
    List<String> supportedParameters,
    Boolean structuredOutputs
) {
    public record Architecture(
        List<String> inputModalities,
        String modality,
        List<String> outputModalities,
        String instructType,
        String tokenizer
    ) {}

    public record Pricing(
        String prompt,
        String completion,
        String request,
        String image
    ) {}
}
