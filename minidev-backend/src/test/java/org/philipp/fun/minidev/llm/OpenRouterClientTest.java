package org.philipp.fun.minidev.llm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OpenRouterClientTest {

    @Test
    void testClientInitializationWithProperties() {
        // Arrange
        LlmProperties properties = new LlmProperties();
        properties.setOpenrouterApiKey("test-key");
        properties.setModel("test-model");
        properties.setBaseUrl("https://openrouter.ai/api/v1/chat/completions");
        
        // Act
        OpenRouterClient client = new OpenRouterClient(properties);

        // Assert
        assertNotNull(client);
    }

    @Test
    void testClientInitializationWithInvalidKey() {
        // Arrange
        LlmProperties properties = new LlmProperties();
        properties.setOpenrouterApiKey("");
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> new OpenRouterClient(properties));
    }
}
