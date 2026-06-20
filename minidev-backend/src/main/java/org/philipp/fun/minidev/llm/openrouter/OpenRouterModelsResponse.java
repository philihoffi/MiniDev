package org.philipp.fun.minidev.llm.openrouter;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from the OpenRouter models endpoint.
 *
 * @param data the list of available models
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenRouterModelsResponse(
        /** The list of available models. */
        List<ModelData> data
) {

    /**
     * Data for a single model.
     *
     * @param id                  the model identifier
     * @param name                the model display name
     * @param description         the model description
     * @param contextLength       the context length in tokens
     * @param created             the creation timestamp
     * @param architecture        the model architecture
     * @param pricing             the pricing information
     * @param supportedParameters the supported parameters
     * @param structuredOutputs   whether structured outputs are supported
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ModelData(
            /** The model identifier. */
            String id,
            /** The model display name. */
            String name,
            /** The model description. */
            String description,
            /** The context length in tokens. */
            @JsonProperty("context_length") Integer contextLength,
            /** The creation timestamp. */
            Long created,
            /** The model architecture. */
            Architecture architecture,
            /** The pricing information. */
            Pricing pricing,
            /** The supported parameters. */
            @JsonProperty("supported_parameters") List<String> supportedParameters,
            /** Whether structured outputs are supported. */
            @JsonProperty("structured_outputs") Boolean structuredOutputs
    ) {
    }

    /**
     * Architecture details for a model.
     *
     * @param inputModalities  the input modalities supported
     * @param modality         the modality type
     * @param outputModalities the output modalities supported
     * @param instructType     the instruct type
     * @param tokenizer        the tokenizer used
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Architecture(
            /** The input modalities supported. */
            @JsonProperty("input_modalities") List<String> inputModalities,
            /** The modality type. */
            String modality,
            /** The output modalities supported. */
            @JsonProperty("output_modalities") List<String> outputModalities,
            /** The instruct type. */
            @JsonProperty("instruct_type") String instructType,
            /** The tokenizer used. */
            String tokenizer
    ) {
    }

    /**
     * Pricing details for a model.
     *
     * @param prompt     the prompt price
     * @param completion the completion price
     * @param request    the request price
     * @param image      the image price
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pricing(
            /** The prompt price. */
            String prompt,
            /** The completion price. */
            String completion,
            /** The request price. */
            String request,
            /** The image price. */
            String image
    ) {
    }
}
