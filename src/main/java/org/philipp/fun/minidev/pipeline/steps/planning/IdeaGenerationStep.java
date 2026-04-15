package org.philipp.fun.minidev.pipeline.steps.planning;

import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.llm.dto.GameIdeas;
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
 * Generate 10-20 ideas for a game based on the provided theme.
 * Each idea should be a short description of a game concept that fits the theme.
 * The ideas should be creative and diverse, covering different genres and gameplay styles.
 * Do not include any implementation details or technical aspects,
 * just focus on the core concept of the game.
 */
public class IdeaGenerationStep extends AbstractStep {

    public IdeaGenerationStep() {
        super("Idea Generation");
    }

    @Override
    protected PipelineResult doExecute(PipelineContext context) throws Exception {
        GameTheme theme = context.getValue(ContextKeys.THEME);
        LlmClient llmClient = context.getValue(ContextKeys.LLM_CLIENT);
        String sessionId = context.getValue(ContextKeys.SESSION_ID);

        if (theme == null) {
            return PipelineResult.failed(getName(), "No theme provided in context", context);
        }
        if (llmClient == null) {
            return PipelineResult.failed(getName(), "No LlmClient provided in context", context);
        }

        JsonSchema schema = new JsonSchema("game_ideas", true, GameIdeas.schema());

        LlmRequest request = new LlmRequest(List.of(
                LlmRequest.Message.system("You are a creative assistant. Generate 10-20 game ideas based on the provided theme."),
                LlmRequest.Message.user("Theme: " + theme.theme())
        ), null, null, schema, sessionId);

        LlmResponse response = llmClient.chat(request);

        if (!response.success()) {
            return PipelineResult.failed(getName(), "LLM API call failed: " + response.errorMessage(), context);
        }

        GameIdeas ideas = response.getContentAs(GameIdeas.class);
        context.putValue(ContextKeys.IDEAS, ideas);
        return PipelineResult.success(getName(), "Idea generation completed: " + ideas.ideas().size() + " ideas generated", context);
    }
}
