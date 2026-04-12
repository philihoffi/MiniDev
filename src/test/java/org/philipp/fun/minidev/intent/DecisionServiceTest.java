package org.philipp.fun.minidev.intent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.llm.LlmRequest;
import org.philipp.fun.minidev.llm.LlmResponse;
import org.philipp.fun.minidev.run.AgentRun;
import org.philipp.fun.minidev.run.AgentRun.RunState;
import org.philipp.fun.minidev.run.GameMetadata;
import org.philipp.fun.minidev.web.service.TerminalSseService;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DecisionServiceTest {

    @Mock
    private LlmClient llmClient;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private TerminalSseService terminalSseService;

    private DecisionService decisionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        decisionService = new DecisionService(llmClient, objectMapper, terminalSseService);
    }

    @Test
    void decideNextStepUsesLlmAndReturnsResponse() throws Exception {
        UUID runId = UUID.randomUUID();
        Path files = Path.of("test-path");
        GameMetadata metadata = new GameMetadata(runId, "Test Game", "Concept", "Mechanic", List.of(), List.of(), files);
        AgentRun run = mock(AgentRun.class);
        when(run.getGameMetadata()).thenReturn(metadata);
        when(run.getState()).thenReturn(RunState.PLANNING);

        String jsonResponse = "{\"runId\":\"" + runId + "\", \"newState\":\"CODING\", \"message\":\"Let's start coding!\", \"accepted\":true}";
        LlmResponse llmResponse = LlmResponse.success(jsonResponse, "test-model", 100);
        when(llmClient.chat(any(LlmRequest.class))).thenReturn(llmResponse);

        DecisionResponse expectedResponse = new DecisionResponse(runId, RunState.CODING, "Let's start coding!", true);
        when(objectMapper.readValue(eq(jsonResponse), eq(DecisionResponse.class))).thenReturn(expectedResponse);

        DecisionResponse result = decisionService.decideNextStep(run);

        assertEquals(RunState.CODING, result.newState());
        assertEquals("Let's start coding!", result.message());
        verify(llmClient).chat(any(LlmRequest.class));
        verify(terminalSseService, atLeastOnce()).sendTerminalText(anyString(), any(), anyInt());
    }

    @Test
    void decideNextStepFallsBackOnException() {
        UUID runId = UUID.randomUUID();
        Path files = Path.of("test-path");
        GameMetadata metadata = new GameMetadata(runId, "Test Game", "Concept", "Mechanic", List.of(), List.of(), files);
        AgentRun run = mock(AgentRun.class);
        when(run.getGameMetadata()).thenReturn(metadata);
        when(run.getState()).thenReturn(RunState.PLANNING);

        when(llmClient.chat(any(LlmRequest.class))).thenThrow(new RuntimeException("LLM error"));

        DecisionResponse result = decisionService.decideNextStep(run);

        assertEquals(RunState.CODING, result.newState());
        assertEquals("Fallback logic used.", result.message());
    }
}
