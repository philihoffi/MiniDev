package org.philipp.fun.minidev.pipeline.steps.planning;

import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.llm.dto.Concepts;
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
 * Select the best game idea from the Elaboration step and evaluate it based on criteria
 * such as originality, feasibility, and fun factor.
 * Provide a justification for the selection and any potential improvements or
 * alternatives that were considered.
 */
public class EvaluationStep extends AbstractStep {

    public EvaluationStep() {
        super("Evaluation");
    }

    @Override
    protected PipelineResult doExecute(PipelineContext context) throws Exception {
        Concepts concepts = context.getValue(ContextKeys.ELABORATED_CONCEPTS);
        LlmClient llmClient = context.getValue(ContextKeys.LLM_CLIENT);
        String sessionId = context.getValue(ContextKeys.SESSION_ID);

        if (concepts == null) {
            return PipelineResult.failed(getName(), "No concepts provided in context");
        }
        if (llmClient == null) {
            return PipelineResult.failed(getName(), "No LlmClient provided in context");
        }

        JsonSchema schema = new JsonSchema("evaluation", true, Evaluation.schema());

        StringBuilder prompt = new StringBuilder("Select the best concept from the following list and evaluate it based on originality, feasibility, and fun factor. Provide a justification and scores.\n");
        for (int i = 0; i < concepts.contents().size(); i++) {
            prompt.append("Concept ").append(i + 1).append(":\n").append(concepts.contents().get(i)).append("\n\n");
        }

        LlmRequest request = new LlmRequest(List.of(
                LlmRequest.Message.system("You are a game design expert. Select the best concept from the provided list and evaluate it based on originality, feasibility, and fun factor. Provide a justification and scores."),
                LlmRequest.Message.user(prompt.toString())
        ), null, null, schema, sessionId);

        LlmResponse response = llmClient.chat(request);

        if (!response.success()) {
            return PipelineResult.failed(getName(), "LLM API call failed: " + response.errorMessage());
        }

        Evaluation evaluation = response.getContentAs(Evaluation.class);
        context.putValue(ContextKeys.EVALUATION, evaluation);
        return PipelineResult.success(getName(), "Evaluation completed: chosen concept " + evaluation.chosenConcept());
    }
}
