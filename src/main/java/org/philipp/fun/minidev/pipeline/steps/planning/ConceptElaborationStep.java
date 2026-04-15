package org.philipp.fun.minidev.pipeline.steps.planning;

import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.llm.dto.Concept;
import org.philipp.fun.minidev.llm.dto.Concepts;
import org.philipp.fun.minidev.llm.dto.GameIdea;
import org.philipp.fun.minidev.llm.dto.GameIdeas;
import org.philipp.fun.minidev.llm.objects.JsonSchema;
import org.philipp.fun.minidev.llm.objects.LlmRequest;
import org.philipp.fun.minidev.llm.objects.LlmResponse;
import org.philipp.fun.minidev.pipeline.abstracts.AbstractStep;
import org.philipp.fun.minidev.pipeline.core.ContextKeys;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.model.PipelineResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Generate a little bit more detailed concept for the narrowed-down game ideas, including core mechanics,
 * setting, and art style. This should be a short paragraph, not more than 5 sentences.
 */
public class ConceptElaborationStep extends AbstractStep {

    public ConceptElaborationStep() {
        super("Concept Elaboration");
    }

    @Override
    protected PipelineResult doExecute(PipelineContext context) throws Exception {
        GameIdeas selectedIdeas = context.getValue(ContextKeys.SELECTED_IDEAS);
        LlmClient llmClient = context.getValue(ContextKeys.LLM_CLIENT);
        String sessionId = context.getValue(ContextKeys.SESSION_ID);

        if (selectedIdeas == null || selectedIdeas.ideas().isEmpty()) {
            return new PipelineResult(getName(), PipelineResult.Status.FAILED, "No selected ideas provided in context", null);
        }
        if (llmClient == null) {
            return new PipelineResult(getName(), PipelineResult.Status.FAILED, "No LlmClient provided in context", null);
        }

        List<String> elaboratedConcepts = new ArrayList<>();
        JsonSchema schema = new JsonSchema("concept", true, Concept.schema());

        for (GameIdea idea : selectedIdeas.ideas()) {
            LlmRequest request = new LlmRequest(List.of(
                    LlmRequest.Message.system("You are a game design expert. Generate a detailed concept for the game idea, including core mechanics, setting, and art style. The response should be a short paragraph, not more than 5 sentences."),
                    LlmRequest.Message.user("Idea: " + idea.name() + "\nHook: " + idea.hook())
            ), null, null, schema, sessionId);

            LlmResponse response = llmClient.chat(request);

            if (!response.success()) {
                return new PipelineResult(getName(), PipelineResult.Status.FAILED, "LLM API call failed: " + response.errorMessage(), null);
            }

            Concept concept = response.getContentAs(Concept.class);
            elaboratedConcepts.add(concept.content());
        }

        context.putValue(ContextKeys.ELABORATED_CONCEPTS, new Concepts(elaboratedConcepts));
        return new PipelineResult(getName(), PipelineResult.Status.SUCCESS, "Concept elaboration completed", null);
    }
}
