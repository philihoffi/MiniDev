package org.philipp.fun.minidev.pipeline.core;

import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.llm.dto.Concepts;
import org.philipp.fun.minidev.llm.dto.Evaluation;
import org.philipp.fun.minidev.llm.dto.GameIdeas;
import org.philipp.fun.minidev.llm.dto.GameTheme;

public class ContextKeys {
    public static final ContextKey<GameTheme> THEME = new ContextKey<>("theme", GameTheme.class);
    public static final ContextKey<GameIdeas> IDEAS = new ContextKey<>("ideas", GameIdeas.class);
    public static final ContextKey<GameIdeas> SELECTED_IDEAS = new ContextKey<>("selectedIdeas", GameIdeas.class);
    public static final ContextKey<Concepts> ELABORATED_CONCEPTS = new ContextKey<>("elaboratedConcepts", Concepts.class);
    public static final ContextKey<Evaluation> EVALUATION = new ContextKey<>("evaluation", Evaluation.class);
    public static final ContextKey<String> DETAILED_DESIGN = new ContextKey<>("detailedDesign", String.class);
    public static final ContextKey<LlmClient> LLM_CLIENT = new ContextKey<>("llmClient", LlmClient.class);
    public static final ContextKey<String> SESSION_ID = new ContextKey<>("sessionId", String.class);
}
