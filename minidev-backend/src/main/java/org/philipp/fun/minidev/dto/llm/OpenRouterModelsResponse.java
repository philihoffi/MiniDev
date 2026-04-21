package org.philipp.fun.minidev.dto.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenRouterModelsResponse(
    List<ModelData> data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ModelData(
        String id,
        String name,
        String description,
        @JsonProperty("context_length") Integer contextLength,
        Long created,
        Architecture architecture,
        Pricing pricing,
        @JsonProperty("supported_parameters") List<String> supportedParameters,
        @JsonProperty("structured_outputs") Boolean structuredOutputs
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Architecture(
        @JsonProperty("input_modalities") List<String> inputModalities,
        String modality,
        @JsonProperty("output_modalities") List<String> outputModalities,
        @JsonProperty("instruct_type") String instructType,
        String tokenizer
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pricing(
        String prompt,
        String completion,
        String request,
        String image
    ) {}
}
