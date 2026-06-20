package org.philipp.fun.minidev.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for LLM integration.
 */
@Configuration
@ConfigurationProperties(prefix = "minidev.llm")
public class LlmProperties {

    /** OpenRouter API key. */
    private String openrouterApiKey;

    /** Default model identifier. */
    private String model = "openrouter/auto";

    /** Base URL for API calls. */
    private String baseUrl = "https://openrouter.ai/api/v1/";

    /**
     * Returns the OpenRouter API key.
     *
     * @return the API key
     */
    public String getOpenrouterApiKey() {
        return openrouterApiKey;
    }

    /**
     * Sets the OpenRouter API key.
     *
     * @param openrouterApiKey the API key
     */
    public void setOpenrouterApiKey(String openrouterApiKey) {
        this.openrouterApiKey = openrouterApiKey;
    }

    /**
     * Returns the default model.
     *
     * @return the model identifier
     */
    public String getModel() {
        return model;
    }

    /**
     * Sets the default model.
     *
     * @param model the model identifier
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * Returns the base URL.
     *
     * @return the base URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Sets the base URL.
     *
     * @param baseUrl the base URL
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}