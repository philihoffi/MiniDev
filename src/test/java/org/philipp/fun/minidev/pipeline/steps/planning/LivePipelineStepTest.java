package org.philipp.fun.minidev.pipeline.steps.planning;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.philipp.fun.minidev.llm.LlmProperties;
import org.philipp.fun.minidev.llm.OpenRouterClient;
import org.philipp.fun.minidev.pipeline.core.ContextKeys;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;
import org.philipp.fun.minidev.pipeline.core.PipelineListener;
import org.philipp.fun.minidev.pipeline.core.Step;
import org.philipp.fun.minidev.pipeline.impl.DefaultPipeline;
import org.philipp.fun.minidev.pipeline.model.PipelineResult;
import org.philipp.fun.minidev.pipeline.stages.PlanningStage;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".+")
public class LivePipelineStepTest {

    @Test
    void testLivePipelineSteps() throws Exception {
        // 1. Setup
        LlmProperties properties = new LlmProperties();
        properties.setOpenrouterApiKey(System.getenv("OPENROUTER_API_KEY"));

        OpenRouterClient llmClient = new OpenRouterClient(properties);
        PipelineContext context = new PipelineContext();
        context.putValue(ContextKeys.LLM_CLIENT, llmClient);
        context.putValue(ContextKeys.SESSION_ID, "live-demo-session: " + UUID.randomUUID());

        // 2. Pipeline Execution
        DefaultPipeline pipeline = new DefaultPipeline("Live Demo");
        pipeline.addListener(new PipelineListener() {
            @Override
            public void onStepStart(PipelineElement step, PipelineContext context) {
                System.out.println(">> Step Started: " + step.getName());
            }

            @Override
            public void onStepEnd(PipelineElement step, PipelineContext context, PipelineResult result) {
                System.out.println("<< Step Ended: " + step.getName() + " [" + result.status() + "]");
            }
        });
        pipeline.addElement(new PlanningStage());

        PipelineResult result = pipeline.execute(context);
        
        System.out.println(result.message());
        assertEquals(PipelineResult.Status.SUCCESS, result.status(), "Pipeline execution failed: " + result.message());

        // 3. Verifications
        assertNotNull(context.getValue(ContextKeys.THEME), "Theme should be in context");
        assertNotNull(context.getValue(ContextKeys.IDEAS), "Ideas should be in context");
        assertNotNull(context.getValue(ContextKeys.SELECTED_IDEAS), "Selected ideas should be in context");
        assertNotNull(context.getValue(ContextKeys.ELABORATED_CONCEPTS), "Elaborated concepts should be in context");
        assertNotNull(context.getValue(ContextKeys.EVALUATION), "Evaluation should be in context");
        assertNotNull(context.getValue(ContextKeys.DETAILED_DESIGN), "Detailed design should be in context");
    }
}
