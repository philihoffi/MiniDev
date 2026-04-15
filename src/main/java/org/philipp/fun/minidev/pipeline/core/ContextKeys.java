package org.philipp.fun.minidev.pipeline.core;

import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.llm.dto.Concepts;
import org.philipp.fun.minidev.llm.dto.Evaluation;
import org.philipp.fun.minidev.llm.dto.GameIdeas;
import org.philipp.fun.minidev.llm.dto.GameTheme;

/**
 * Central registry for all context keys used within the pipeline.
 * Keys are organized into namespaces to facilitate categorized access and better readability.
 */
public class ContextKeys {

    /**
     * Keys related to the planning phase of the pipeline.
     */
    public static final class PlanningStage {
        /** Key for the generated game theme. */
        public static final ContextKey<GameTheme> THEME = new ContextKey<>("theme", GameTheme.class);
        /** Key for the full list of generated game ideas. */
        public static final ContextKey<GameIdeas> IDEAS_ALL = new ContextKey<>("ideas", GameIdeas.class);
        /** Key for the selected subset of game ideas. */
        public static final ContextKey<GameIdeas> IDEAS_SELECTED = new ContextKey<>("selectedIdeas", GameIdeas.class);
        /** Key for elaborated game concepts. */
        public static final ContextKey<Concepts> ELABORATED_CONCEPTS = new ContextKey<>("elaboratedConcepts", Concepts.class);
        /** Key for the evaluation of a selected game concept. */
        public static final ContextKey<Evaluation> EVALUATION = new ContextKey<>("evaluation", Evaluation.class);
        /** Key for the detailed design document string. */
        public static final ContextKey<String> DETAILED_DESIGN = new ContextKey<>("detailedDesign", String.class);
    }

    /**
     * System-level keys used by the pipeline infrastructure.
     */
    public static final class System {
        /** Key for the LLM client instance. */
        public static final ContextKey<LlmClient> LLM_CLIENT = new ContextKey<>("llmClient", LlmClient.class);
        /** Key for the current session identifier. */
        public static final ContextKey<String> SESSION_ID = new ContextKey<>("sessionId", String.class);
    }
}
