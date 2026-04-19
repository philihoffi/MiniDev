package org.philipp.fun.minidev.llm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OpenRouterClientTest {

    @Test
    void testClientInitializationWithProperties() {
        LlmProperties properties = new LlmProperties();
        properties.setOpenrouterApiKey("test-key");
        properties.setModel("test-model");
        properties.setBaseUrl("https://openrouter.ai/api/v1/chat/completions");
        
        OpenRouterClient client = new OpenRouterClient(properties);
        assertNotNull(client);
    }

    @Test
    void testClientInitializationWithInvalidKey() {
        LlmProperties properties = new LlmProperties();
        properties.setOpenrouterApiKey("");
        
        assertThrows(IllegalArgumentException.class, () -> new OpenRouterClient(properties));
    }
}
