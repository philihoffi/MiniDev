package org.philipp.fun.minidev.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.philipp.fun.minidev.llm.objects.LlmRequest;
import org.philipp.fun.minidev.llm.objects.LlmResponse;
import org.philipp.fun.minidev.llm.objects.OpenRouterRequest;
import org.philipp.fun.minidev.llm.objects.OpenRouterResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Objects;

@Component
public class OpenRouterClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterClient.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestClient restClient;
    private final LlmProperties properties;

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

            OpenRouterRequest requestBody = new OpenRouterRequest(
                    messages,
                    properties.getModel(),
                    request.temperature(),
                    request.maxTokens(),
                    responseFormat,
                    request.sessionId()
            );

            String requestJson = OBJECT_MAPPER.writeValueAsString(requestBody);
            log.info("Sending chat request to OpenRouter. Model: {}, Messages: {}", requestBody.model(), messages.size());
            log.debug("Request body: {}", requestJson);
            String responseBody = restClient.post()
                    .body(requestJson)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null) {
                log.warn("Received empty response from OpenRouter API");
                return LlmResponse.failure("Empty response from OpenRouter");
            }
            responseBody = responseBody.trim();
            log.debug("Received response from OpenRouter: {}", responseBody);

            OpenRouterResponse response = OBJECT_MAPPER.readValue(responseBody, OpenRouterResponse.class);

            if (response.choices() == null || response.choices().isEmpty()) {
                return LlmResponse.failure("No choices in response");
            }

            OpenRouterResponse.Choice firstChoice = response.choices().getFirst();
            String content = firstChoice.message().content();
            Integer totalTokens = response.usage() != null ? response.usage().totalTokens() : null;
            String model = response.model();

            return LlmResponse.success(content, model, totalTokens);

        } catch (Exception e) {
            log.error("Error calling OpenRouter API", e);
            return LlmResponse.failure("API call failed: " + e.getMessage());
        }
    }

}
