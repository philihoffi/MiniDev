package org.philipp.fun.minidev.pipeline.steps.planning;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.philipp.fun.minidev.llm.LlmProperties;
import org.philipp.fun.minidev.llm.OpenRouterClient;
import org.philipp.fun.minidev.llm.dto.GameTheme;
import org.philipp.fun.minidev.pipeline.core.ContextKeys;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;
import org.philipp.fun.minidev.pipeline.core.PipelineListener;
import org.philipp.fun.minidev.pipeline.core.Step;
import org.philipp.fun.minidev.pipeline.impl.DefaultPipeline;
import org.philipp.fun.minidev.pipeline.model.PipelineResult;
import org.philipp.fun.minidev.pipeline.stages.PlanningStage;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".+")
public class LivePipelineStepTest {

    @Test
    void testLivePipelineSteps() throws Exception {
        // 1. Setup
        LlmProperties properties = new LlmProperties();
        properties.setOpenrouterApiKey(System.getenv("OPENROUTER_API_KEY"));

        OpenRouterClient llmClient = new OpenRouterClient(properties);
        PipelineContext context = new PipelineContext();
        context.putValue(ContextKeys.System.LLM_CLIENT, llmClient);
        context.putValue(ContextKeys.System.SESSION_ID, "live-demo-session: " + UUID.randomUUID());

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
        assertNotNull(context.getValue(ContextKeys.PlanningStage.THEME), "Theme should be in context");
        assertNotNull(context.getValue(ContextKeys.PlanningStage.IDEAS_ALL), "Ideas should be in context");
        assertNotNull(context.getValue(ContextKeys.PlanningStage.IDEAS_SELECTED), "Selected ideas should be in context");
        assertNotNull(context.getValue(ContextKeys.PlanningStage.ELABORATED_CONCEPTS), "Elaborated concepts should be in context");
        assertNotNull(context.getValue(ContextKeys.PlanningStage.EVALUATION), "Evaluation should be in context");
        assertNotNull(context.getValue(ContextKeys.PlanningStage.DETAILED_DESIGN), "Detailed design should be in context");

        System.out.println("Detailed Design: " + context.getValue(ContextKeys.PlanningStage.DETAILED_DESIGN));
    }

    @Test
    void testThemeGenerationStepLive() throws Exception {
        // 1. Setup
        LlmProperties properties = new LlmProperties();
        properties.setOpenrouterApiKey(System.getenv("OPENROUTER_API_KEY"));

        OpenRouterClient llmClient = new OpenRouterClient(properties);
        PipelineContext context = new PipelineContext();
        context.putValue(ContextKeys.System.LLM_CLIENT, llmClient);
        context.putValue(ContextKeys.System.SESSION_ID, "live-theme-gen-test: " + UUID.randomUUID());

        // 2. Execution
        ThemeGenerationStep step = new ThemeGenerationStep();
        PipelineResult result = step.execute(context);

        // 3. Verifications
        System.out.println("Result Message: " + result.message());
        assertEquals(PipelineResult.Status.SUCCESS, result.status(), "Step execution failed: " + result.message());

        GameTheme theme = context.getValue(ContextKeys.PlanningStage.THEME);
        assertNotNull(theme, "Theme should be in context");
        assertNotNull(theme.theme(), "Theme string should not be null");

        String themeStr = theme.theme();
        System.out.println("Generated Theme: " + themeStr);

        // Verification of the 5 lowercase words, no punctuation constraint
        // The pattern in GameTheme is ^[a-z]+( [a-z]+){4}$
        assertTrue(themeStr.matches("^[a-z]+( [a-z]+){4}$"),
                "Theme should consist of exactly 5 lowercase words separated by single spaces, but was: " + themeStr);
    }
}
