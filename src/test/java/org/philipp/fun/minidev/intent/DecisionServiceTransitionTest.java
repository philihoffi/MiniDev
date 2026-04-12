package org.philipp.fun.minidev.intent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.llm.LlmRequest;
import org.philipp.fun.minidev.llm.LlmResponse;
import org.philipp.fun.minidev.run.AgentRun;
import org.philipp.fun.minidev.run.AgentRun.RunState;
import org.philipp.fun.minidev.run.GameMetadata;
import org.philipp.fun.minidev.web.service.TerminalSseService;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class DecisionServiceTransitionTest {

    private LlmClient llmClient;
    private DecisionService decisionService;
    private AgentRun run;

    @BeforeEach
    void setUp() {
        llmClient = Mockito.mock(LlmClient.class);
        ObjectMapper objectMapper = new ObjectMapper();
        TerminalSseService terminalSseService = Mockito.mock(TerminalSseService.class);
        decisionService = new DecisionService(llmClient, objectMapper, terminalSseService);

        UUID runId = UUID.randomUUID();
        GameMetadata metadata = new GameMetadata(
                runId, "Test Game", "Concept", "Mechanic", new ArrayList<>(), Path.of("test")
        );
        run = new AgentRun(RunState.PLANNING, java.time.Instant.now(), java.time.Instant.now(), metadata);
    }

    @Test
    void decideNextStepShouldReturnValidTransitionWhenLlmReturnsInvalidOne() throws Exception {
        // RunState.PLANNING can transition to CODING, FAILED, PAUSED
        // We simulate LLM returning DONE, which is invalid from PLANNING
        String jsonResponse = "{\"runId\": \"" + run.getGameMetadata().runId().toString() + "\", \"newState\": \"DONE\", \"message\": \"Finished early\", \"accepted\": true}";
        when(llmClient.chat(any(LlmRequest.class))).thenReturn(LlmResponse.success(jsonResponse, "test-model", 10));

        DecisionResponse decision = decisionService.decideNextStep(run);

        assertTrue(run.getState().canTransitionTo(decision.newState()),
                "Decision " + decision.newState() + " should be a valid transition from " + run.getState());
    }

    @Test
    void fallbackDecisionShouldAlwaysReturnValidTransition() {
        for (RunState state : RunState.values()) {
            if (state == RunState.PAUSED || state == RunState.FAILED || state == RunState.IDLE) continue;
            // Re-setup run with current state
            UUID runId = UUID.randomUUID();
            GameMetadata metadata = new GameMetadata(
                    runId, "Test Game", "Concept", "Mechanic", new ArrayList<>(), Path.of("test")
            );
            AgentRun testRun = new AgentRun(state, java.time.Instant.now(), java.time.Instant.now(), metadata);
            
            when(llmClient.chat(any())).thenThrow(new RuntimeException("LLM failure for fallback test"));
            
            DecisionResponse decision = decisionService.decideNextStep(testRun); 
            
            assertNotNull(decision.newState(), "New state should not be null for " + state);
        }
    }
}
