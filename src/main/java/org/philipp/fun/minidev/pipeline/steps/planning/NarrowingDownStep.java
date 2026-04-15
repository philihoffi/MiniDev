package org.philipp.fun.minidev.pipeline.steps.planning;

import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.llm.dto.GameIdea;
import org.philipp.fun.minidev.llm.dto.GameIdeas;
import org.philipp.fun.minidev.llm.objects.JsonSchema;
import org.philipp.fun.minidev.llm.objects.LlmRequest;
import org.philipp.fun.minidev.llm.objects.LlmResponse;
import org.philipp.fun.minidev.pipeline.abstracts.AbstractStep;
import org.philipp.fun.minidev.pipeline.core.ContextKeys;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.model.PipelineResult;

import java.util.Comparator;
import java.util.List;

/**
 * Narrow down the generated GameIdeas to the 3 most promising ones.
 */
public class NarrowingDownStep extends AbstractStep {

    public NarrowingDownStep() {
        super("Narrowing Down");
    }

    @Override
    protected PipelineResult doExecute(PipelineContext context) throws Exception {
        GameIdeas allIdeas = context.getValue(ContextKeys.PlanningStage.IDEAS_ALL);
        LlmClient llmClient = context.getValue(ContextKeys.System.LLM_CLIENT);
        String sessionId = context.getValue(ContextKeys.System.SESSION_ID);

        if (allIdeas == null) {
            return PipelineResult.failed(getName(), "No ideas provided in context", context);
        }
        if (llmClient == null) {
            return PipelineResult.failed(getName(), "No LlmClient provided in context", context);
        }

        JsonSchema schema = new JsonSchema("game_ideas", true, GameIdeas.schema());

        StringBuilder prompt = new StringBuilder("Select the 3 most promising game ideas from the following list:\n");
        for (GameIdea idea : allIdeas.ideas()) {
            prompt.append("- ").append(idea.name()).append(": ").append(idea.hook()).append("\n");
        }

        LlmRequest request = new LlmRequest(List.of(
                LlmRequest.Message.system("You are a game design expert. Select the 3 most promising game ideas from the provided list."),
                LlmRequest.Message.user(prompt.toString())
        ), null, null, schema, sessionId);

        LlmResponse response = llmClient.chat(request);

        if (!response.success()) {
            return PipelineResult.failed(getName(), "LLM API call failed: " + response.errorMessage(), context);
        }

        GameIdeas selectedIdeas = response.getContentAs(GameIdeas.class);

        if (selectedIdeas.ideas().size() > 3) {
            notifyWarning(this, context, "LLM returned more than 3 ideas, using fallback to select the top 3.");
            selectedIdeas = new GameIdeas(selectedIdeas.ideas().stream().sorted(Comparator.comparing(GameIdea::feasibility).reversed()).limit(3).toList());
        }

        context.putValue(ContextKeys.PlanningStage.IDEAS_SELECTED, selectedIdeas);
        return PipelineResult.success(getName(), "Narrowing down completed: selected " + selectedIdeas.ideas().size() + " ideas", context);
    }
}
