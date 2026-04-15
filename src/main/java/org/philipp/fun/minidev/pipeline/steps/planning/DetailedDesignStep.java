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
import org.philipp.fun.minidev.pipeline.model.StepResult;

import java.util.List;
import java.util.Map;

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
    protected StepResult doExecute(PipelineContext context) throws Exception {
        Evaluation evaluation = context.getValue(ContextKeys.EVALUATION);
        LlmClient llmClient = context.getValue(ContextKeys.LLM_CLIENT);
        String sessionId = context.getValue(ContextKeys.SESSION_ID);

        if (evaluation == null) {
            return new StepResult(StepResult.StepStatus.FAILED, "Evaluation not provided in context");
        }
        if (llmClient == null) {
            return new StepResult(StepResult.StepStatus.FAILED, "No LlmClient provided in context");
        }

        JsonSchema schema = new JsonSchema("design", true, Design.schema());

        LlmRequest request = new LlmRequest(List.of(
                LlmRequest.Message.system("You are a game design expert. Create a formal Game Design Document (GDD) in Markdown format. The document must be structured with the following sections: 1. Game Title, 2. High Concept, 3. Core Mechanics, 4. Story/Lore, 5. Visual and Audio Style, 6. Key Gameplay Loop. Do not include implementation details or code."),
                LlmRequest.Message.user("Concept: " + evaluation.chosenConcept() + "\n\nEvaluation: " + evaluation.justification())
        ), null, null, schema, sessionId);

        LlmResponse response = llmClient.chat(request);

        if (!response.success()) {
            return new StepResult(StepResult.StepStatus.FAILED, "LLM API call failed: " + response.errorMessage());
        }

        Design design = response.getContentAs(Design.class);
        context.putValue(ContextKeys.DETAILED_DESIGN, design.content());
        return new StepResult(StepResult.StepStatus.SUCCESS, "Detailed design completed");
    }
}
