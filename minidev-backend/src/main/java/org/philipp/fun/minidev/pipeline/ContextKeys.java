package org.philipp.fun.minidev.pipeline;

import org.philipp.fun.minidev.dto.CreativeBrief;
import org.philipp.fun.minidev.llm.LlmClient;

public class ContextKeys {
    public static final class System {
        public static final ContextKey<LlmClient> LLM_CLIENT = new ContextKey<>("llmClient", LlmClient.class);
    }

    public static final class Wallpaper {
        public static final ContextKey<String> THEME = new ContextKey<>("theme", String.class);
        public static final ContextKey<CreativeBrief> CREATIVE_BRIEF = new ContextKey<>("creativeBrief", CreativeBrief.class);
        public static final ContextKey<String> CODE = new ContextKey<>("code", String.class);
        public static final ContextKey<String> REVIEW_FEEDBACK = new ContextKey<>("reviewFeedback", String.class);
        public static final ContextKey<Boolean> REVIEW_PASSED = new ContextKey<>("reviewPassed", Boolean.class);
    }
}
