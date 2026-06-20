package org.philipp.fun.minidev.pipeline.core;

import java.util.Map;

import org.philipp.fun.minidev.llm.client.LlmClient;
import org.philipp.fun.minidev.wallpaper.dto.CreativeBrief;

/**
 * Central registry of pipeline context keys.
 */
public class ContextKeys {

    /**
     * System-level context keys.
     */
    public static final class System {

        /** LLM client for pipeline execution. */
        public static final ContextKey<LlmClient> LLM_CLIENT =
                new ContextKey<>("llmClient", LlmClient.class);

        /** LLM configuration map. */
        public static final ContextKey<Map<String, Object>> LLM_CONFIG =
                new ContextKey<>("llmConfig", (Class<Map<String, Object>>) (Class<?>) Map.class);

        /** Private constructor for utility class. */
        private System() {
        }
    }

    /**
     * Wallpaper pipeline context keys.
     */
    public static final class Wallpaper {

        /** Theme string. */
        public static final ContextKey<String> THEME =
                new ContextKey<>("theme", String.class);

        /** Creative brief. */
        public static final ContextKey<CreativeBrief> CREATIVE_BRIEF =
                new ContextKey<>("creativeBrief", CreativeBrief.class);

        /** Generated code. */
        public static final ContextKey<String> CODE =
                new ContextKey<>("code", String.class);

        /** Review feedback string. */
        public static final ContextKey<String> REVIEW_FEEDBACK =
                new ContextKey<>("reviewFeedback", String.class);

        /** Whether the review passed. */
        public static final ContextKey<Boolean> REVIEW_PASSED =
                new ContextKey<>("reviewPassed", Boolean.class);

        /** Private constructor for utility class. */
        private Wallpaper() {
        }
    }

    /** Private constructor for utility class. */
    private ContextKeys() {
    }
}