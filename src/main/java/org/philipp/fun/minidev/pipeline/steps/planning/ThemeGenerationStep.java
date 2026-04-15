package org.philipp.fun.minidev.pipeline.steps.planning;

import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.llm.dto.GameTheme;
import org.philipp.fun.minidev.llm.objects.JsonSchema;
import org.philipp.fun.minidev.llm.objects.LlmRequest;
import org.philipp.fun.minidev.llm.objects.LlmResponse;
import org.philipp.fun.minidev.pipeline.abstracts.AbstractStep;
import org.philipp.fun.minidev.pipeline.core.ContextKeys;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.model.PipelineResult;

import java.util.List;

/**
 * Generate exactly one Game Jam theme with exactly 5 lowercase words.
 * No punctuation.
 */
public class ThemeGenerationStep extends AbstractStep {

    private String fixedTheme;
    
    public ThemeGenerationStep() {
        super("Theme Generation");
        this.fixedTheme = null;
    }
    
    public ThemeGenerationStep(String theme) {
        this();
        this.fixedTheme = theme;
    }

    @Override
    protected PipelineResult doExecute(PipelineContext context) throws Exception {
        if (fixedTheme != null) {
            GameTheme theme = new GameTheme(fixedTheme);
            context.putValue(ContextKeys.THEME, theme);
            return PipelineResult.success(getName(), "Using fixed theme: " + fixedTheme, context);
        }

        LlmClient llmClient = context.getValue(ContextKeys.LLM_CLIENT);
        String sessionId = context.getValue(ContextKeys.SESSION_ID);

        if (llmClient == null) {
            return PipelineResult.failed(getName(), "No LlmClient provided in context", context);
        }

        JsonSchema schema = new JsonSchema("game_theme", true, GameTheme.schema());

        LlmRequest request = new LlmRequest(List.of(
                LlmRequest.Message.system("Generate exactly one Game Jam theme with exactly 5 lowercase words. No punctuation."),
                LlmRequest.Message.user("Generate a theme for a new game.")
        ), 2.0, null, schema, sessionId);

        LlmResponse response = llmClient.chat(request);

        if (!response.success()) {
            return PipelineResult.failed(getName(), "LLM API call failed: " + response.errorMessage(), context);
        }

        GameTheme theme = response.getContentAs(GameTheme.class);
        context.putValue(ContextKeys.THEME, theme);
        return PipelineResult.success(getName(), "Theme generation completed: " + theme.theme(), context);
    }
}
