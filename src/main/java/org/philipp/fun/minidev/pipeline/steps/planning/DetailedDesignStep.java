package org.philipp.fun.minidev.pipeline.steps.planning;

import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.llm.dto.Design;
import org.philipp.fun.minidev.llm.dto.Evaluation;
import org.philipp.fun.minidev.llm.objects.JsonSchema;
import org.philipp.fun.minidev.llm.objects.LlmRequest;
import org.philipp.fun.minidev.llm.objects.LlmResponse;
import org.philipp.fun.minidev.pipeline.abstracts.AbstractStep;
import org.philipp.fun.minidev.pipeline.core.ContextKeys;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.model.PipelineResult;

import java.util.List;

/**
 * Create a detailed design for the selected game idea, including a game design document,
 * character designs, and level layouts. This should be a comprehensive document that
 * outlines the game's mechanics, story, and visual style.
 * But no Code or how to Implement this.
 */
public class DetailedDesignStep extends AbstractStep {

    public DetailedDesignStep() {
        super("Detailed Design");
    }

    @Override
    protected PipelineResult doExecute(PipelineContext context) throws Exception {
        Evaluation evaluation = context.getValue(ContextKeys.EVALUATION);
        LlmClient llmClient = context.getValue(ContextKeys.LLM_CLIENT);
        String sessionId = context.getValue(ContextKeys.SESSION_ID);

        if (evaluation == null) {
            return new PipelineResult(getName(), PipelineResult.Status.FAILED, "Evaluation not provided in context", null);
        }
        if (llmClient == null) {
            return new PipelineResult(getName(), PipelineResult.Status.FAILED, "No LlmClient provided in context", null);
        }

        JsonSchema schema = new JsonSchema("design", true, Design.schema());

        LlmRequest request = new LlmRequest(List.of(
                LlmRequest.Message.system("You are a game design expert. Create a detailed design for the game, including game mechanics, story, and visual style. Do not include implementation details or code."),
                LlmRequest.Message.user("Concept: " + evaluation.chosenConcept() + "\n\nEvaluation: " + evaluation.justification())
        ), null, null, schema, sessionId);

        LlmResponse response = llmClient.chat(request);

        if (!response.success()) {
            return new PipelineResult(getName(), PipelineResult.Status.FAILED, "LLM API call failed: " + response.errorMessage(), null);
        }

        Design design = response.getContentAs(Design.class);
        context.putValue(ContextKeys.DETAILED_DESIGN, design.content());
        return new PipelineResult(getName(), PipelineResult.Status.SUCCESS, "Detailed design completed", null);
    }
}
