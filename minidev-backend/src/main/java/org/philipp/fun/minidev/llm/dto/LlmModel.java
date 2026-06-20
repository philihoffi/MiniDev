package org.philipp.fun.minidev.llm.dto;

import java.util.List;

/**
 * DTO representing an LLM model with its architecture and pricing details.
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
public record LlmModel(
        /** The model identifier. */
        String id,
        /** The model display name. */
        String name,
        /** The model description. */
        String description,
        /** The context length in tokens. */
        Integer contextLength,
        /** The creation timestamp. */
        Long created,
        /** The model architecture. */
        Architecture architecture,
        /** The pricing information. */
        Pricing pricing,
        /** The supported parameters. */
        List<String> supportedParameters,
        /** Whether structured outputs are supported. */
        Boolean structuredOutputs
) {

    /**
     * Architecture details for a model.
     *
     * @param inputModalities  the input modalities
     * @param modality         the modality type
     * @param outputModalities the output modalities
     * @param instructType     the instruct type
     * @param tokenizer        the tokenizer used
     */
    public record Architecture(
            /** The input modalities. */
            List<String> inputModalities,
            /** The modality type. */
            String modality,
            /** The output modalities. */
            List<String> outputModalities,
            /** The instruct type. */
            String instructType,
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