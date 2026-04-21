package org.philipp.fun.minidev.llm;

import org.junit.jupiter.api.Test;
import org.philipp.fun.minidev.dto.llm.LlmResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LlmResponseTest {

    record TestContent(String name, int age) {}

    @Test
    void testGetContentAsSuccess() {
        // Arrange
        String json = "{\"name\":\"Philipp\", \"age\":30}";
        LlmResponse response = LlmResponse.success(json, "test-model", 10);
        
        // Act
        TestContent content = response.getContentAs(TestContent.class);
        
        // Assert
        assertEquals("Philipp", content.name());
        assertEquals(30, content.age());
    }

    @Test
    void testGetContentAsFailure() {
        // Arrange
        LlmResponse response = LlmResponse.success("invalid-json", "test-model", 10);
        
        // Act & Assert
        assertThrows(RuntimeException.class, () -> response.getContentAs(TestContent.class));
    }
}
