package org.philipp.fun.minidev.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpenRouterClientTest {

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private OpenRouterClient client;

    @BeforeEach
    void setUp() {
        client = new OpenRouterClient("test-api-key");
    }

    @Test
    void chatShouldReturnSuccessResponse() throws Exception {
        String mockResponse = """
                {
                    "id": "test-id",
                    "model": "anthropic/claude-3.5-sonnet",
                    "choices": [
                        {
                            "message": {
                                "role": "assistant",
                                "content": "Hello, how can I help you?"
                            }
                        }
                    ],
                    "usage": {
                        "total_tokens": 50
                    }
                }
                """;

        RestClient mockRestClient = mock(RestClient.class);
        when(mockRestClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(anyString())).thenAnswer(invocation -> {
            String requestJson = invocation.getArgument(0);
            assertTrue(requestJson.contains("\"session_id\":\"test-session-id\""), "Request should contain session_id");
            return requestBodySpec;
        });
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(mockResponse);

        // Use reflection to inject mock RestClient
        Field restClientField = OpenRouterClient.class.getDeclaredField("restClient");
        restClientField.setAccessible(true);
        restClientField.set(client, mockRestClient);

        LlmRequest request = new LlmRequest(
                List.of(LlmRequest.Message.user("Hello")),
                null,
                null,
                false,
                null,
                "test-session-id"
        );
        LlmResponse response = client.chat(request);

        assertTrue(response.success());
        assertEquals("Hello, how can I help you?", response.content());
        assertEquals("anthropic/claude-3.5-sonnet", response.model());
        assertEquals(50, response.tokensUsed());
    }

    @Test
    void chatShouldReturnFailureWhenChoicesAreEmpty() throws Exception {
        String mockResponse = """
                {
                    "choices": []
                }
                """;

        RestClient mockRestClient = mock(RestClient.class);
        when(mockRestClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(mockResponse);

        Field restClientField = OpenRouterClient.class.getDeclaredField("restClient");
        restClientField.setAccessible(true);
        restClientField.set(client, mockRestClient);

        LlmRequest request = new LlmRequest(List.of(LlmRequest.Message.user("Hello")));
        LlmResponse response = client.chat(request);

        assertFalse(response.success());
        assertEquals("No choices in response", response.errorMessage());
    }
}
