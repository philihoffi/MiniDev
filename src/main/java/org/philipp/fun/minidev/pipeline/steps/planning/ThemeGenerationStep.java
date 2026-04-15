package org.philipp.fun.minidev.pipeline.steps.planning;

import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.llm.dto.GameTheme;
import org.philipp.fun.minidev.llm.objects.JsonSchema;
import org.philipp.fun.minidev.llm.objects.LlmRequest;
import org.philipp.fun.minidev.llm.objects.LlmResponse;
import org.philipp.fun.minidev.pipeline.abstracts.AbstractStep;
import org.philipp.fun.minidev.pipeline.core.ContextKeys;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.model.StepResult;

import java.util.List;

/**
 * Generate exactly one Game Jam theme with exactly 5 lowercase words.
 * No punctuation.
 */
public class ThemeGenerationStep extends AbstractStep {

    public ThemeGenerationStep() {
        super("Theme Generation");
    }

    @Override
    protected StepResult doExecute(PipelineContext context) throws Exception {
        LlmClient llmClient = context.getValue(ContextKeys.LLM_CLIENT);
        String sessionId = context.getValue(ContextKeys.SESSION_ID);

        if (llmClient == null) {
            return new StepResult(StepResult.StepStatus.FAILED, "No LlmClient provided in context");
        }

        JsonSchema schema = new JsonSchema("game_theme", true, GameTheme.schema());

        LlmRequest request = new LlmRequest(List.of(
                LlmRequest.Message.system("Generate exactly one Game Jam theme with exactly 5 lowercase words. No punctuation."),
                LlmRequest.Message.user("Generate a theme for a new game.")
        ), null, null, schema, sessionId);

        LlmResponse response = llmClient.chat(request);

        if (!response.success()) {
            return new StepResult(StepResult.StepStatus.FAILED, "LLM API call failed: " + response.errorMessage());
        }

        GameTheme theme = response.getContentAs(GameTheme.class);
        context.putValue(ContextKeys.THEME, theme);
        return new StepResult(StepResult.StepStatus.SUCCESS, "Theme generation completed: " + theme.theme());
    }
}
