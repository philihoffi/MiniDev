package org.philipp.fun.minidev.llm.openrouter;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.philipp.fun.minidev.llm.client.LlmClient;
import org.philipp.fun.minidev.llm.client.LlmProperties;
import org.philipp.fun.minidev.llm.dto.LlmModel;
import org.philipp.fun.minidev.llm.dto.LlmRequest;
import org.philipp.fun.minidev.llm.dto.LlmResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * OpenRouter API client for LLM interactions.
 */
@Component
public class OpenRouterClient implements LlmClient {

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(OpenRouterClient.class);

    /** Object mapper for JSON serialization/deserialization. */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** Maximum length for error response body logging. */
    private static final int MAX_ERROR_BODY_LENGTH = 600;

    /** REST client for API calls. */
    private final RestClient restClient;

    /** LLM configuration properties. */
    private final LlmProperties properties;

    /**
     * Constructs an OpenRouterClient.
     *
     * @param properties the LLM properties
     */
    public OpenRouterClient(LlmProperties properties) {
        this.properties = properties;
        String apiKey = properties.getOpenrouterApiKey();
        Objects.requireNonNull(apiKey, "apiKey must not be null");
        if (apiKey.isBlank()) {
            throw new IllegalArgumentException("apiKey must not be blank");
        }
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Sends a chat completion request to OpenRouter.
     *
     * @param request the LLM request
     * @return the LLM response
     */
    @Override
    public LlmResponse chat(LlmRequest request) {
        try {
            List<OpenRouterRequest.Message> messages = request.messages().stream()
                    .map(m -> new OpenRouterRequest.Message(m.role(), m.content()))
                    .toList();

            OpenRouterRequest.ResponseFormat responseFormat = null;
            if (request.jsonSchema() != null) {
                responseFormat = new OpenRouterRequest.ResponseFormat(
                        "json_schema",
                        request.jsonSchema()
                );
            }

            String model = request.model() != null ? request.model() : properties.getModel();

            OpenRouterRequest requestBody = new OpenRouterRequest(
                    messages,
                    model,
                    request.temperature(),
                    request.maxTokens(),
                    responseFormat,
                    request.sessionId()
            );

            String requestJson = OBJECT_MAPPER.writeValueAsString(requestBody);
            LOG.info("openrouter_chat_request model={} messages={} maxTokens={} temperature={} schema={}",
                    requestBody.model(),
                    messages.size(),
                    request.maxTokens(),
                    request.temperature(),
                    request.jsonSchema() != null);
            String responseBody = restClient.post()
                    .uri("chat/completions")
                    .body(requestJson)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (req, res) ->
                            LOG.error("openrouter_chat_error status={} reason={} body={}",
                                    res.getStatusCode(),
                                    res.getStatusText(),
                                    abbreviate(new String(res.getBody().readAllBytes()))))
                    .body(String.class);

            if (responseBody == null) {
                LOG.warn("Received empty response from OpenRouter API");
                return LlmResponse.failure("Empty response from OpenRouter");
            }
            responseBody = responseBody.trim();
            LOG.debug("openrouter_chat_response_received size={} chars", responseBody.length());

            OpenRouterResponse response = OBJECT_MAPPER.readValue(responseBody, OpenRouterResponse.class);

            if (response.choices() == null || response.choices().isEmpty()) {
                return LlmResponse.failure("No choices in response");
            }

            OpenRouterResponse.Choice firstChoice = response.choices().getFirst();
            String content = firstChoice.message().content();
            Integer totalTokens = response.usage() != null ? response.usage().totalTokens() : null;
            String responseModel = response.model();

            LOG.info("openrouter_chat_success model={} tokens={}", responseModel, totalTokens);

            return LlmResponse.success(content, responseModel, totalTokens);

        } catch (Exception e) {
            LOG.error("Error calling OpenRouter API", e);
            return LlmResponse.failure("API call failed: " + e.getMessage());
        }
    }

    /**
     * Fetches available models from OpenRouter.
     *
     * @return list of available models
     */
    @Override
    public List<LlmModel> getModels() {
        return getModels(null, null, null);
    }

    /**
     * Fetches models with optional filters.
     *
     * @param category            optional category filter
     * @param supportedParameters optional supported parameters filter
     * @param outputModalities    optional output modalities filter
     * @return list of filtered models
     */
    @Override
    public List<LlmModel> getModels(String category, String supportedParameters, String outputModalities) {
        try {
            LOG.info("openrouter_models_request baseUrl={} category={} supportedParameters={} outputModalities={}",
                    properties.getBaseUrl(), category, supportedParameters, outputModalities);

            String responseBody = restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("models");
                        if (category != null) {
                            uriBuilder.queryParam("category", category);
                        }
                        if (supportedParameters != null) {
                            uriBuilder.queryParam("supported_parameters", supportedParameters);
                        }
                        if (outputModalities != null) {
                            uriBuilder.queryParam("output_modalities", outputModalities);
                        }
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (req, res) ->
                            LOG.error("openrouter_models_error status={} reason={} body={}",
                                    res.getStatusCode(),
                                    res.getStatusText(),
                                    abbreviate(new String(res.getBody().readAllBytes()))))
                    .body(String.class);

            if (responseBody == null) {
                LOG.warn("Received empty response when fetching models");
                return List.of();
            }

            OpenRouterModelsResponse response = OBJECT_MAPPER.readValue(responseBody, OpenRouterModelsResponse.class);
            if (response.data() == null) {
                return List.of();
            }

            List<LlmModel> models = response.data().stream()
                    .map(m -> new LlmModel(
                            m.id(),
                            m.name(),
                            m.description(),
                            m.contextLength(),
                            m.created(),
                            m.architecture() != null ? new LlmModel.Architecture(
                                    m.architecture().inputModalities(),
                                    m.architecture().modality(),
                                    m.architecture().outputModalities(),
                                    m.architecture().instructType(),
                                    m.architecture().tokenizer()
                            ) : null,
                            m.pricing() != null ? new LlmModel.Pricing(
                                    m.pricing().prompt(),
                                    m.pricing().completion(),
                                    m.pricing().request(),
                                    m.pricing().image()
                            ) : null,
                            m.supportedParameters(),
                            m.structuredOutputs()
                    ))
                    .toList();

            LOG.info("openrouter_models_success count={}", models.size());
            return models;
        } catch (Exception e) {
            LOG.error("Error fetching models from OpenRouter", e);
            return List.of();
        }
    }

    /**
     * Abbreviates a string to a maximum length.
     *
     * @param value the input string
     * @return the abbreviated string
     */
    private static String abbreviate(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() <= MAX_ERROR_BODY_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_BODY_LENGTH) + "...";
    }

}