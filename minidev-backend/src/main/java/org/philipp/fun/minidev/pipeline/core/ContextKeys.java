package org.philipp.fun.minidev.pipeline.core;

import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.model.AgentRun;

/**
 * Central registry for all context keys used within the pipeline.
 * Keys are organized into namespaces to facilitate categorized access and better readability.
 */
public class ContextKeys {
    /**
     * System-level keys used by the pipeline infrastructure.
     */
    public static final class System {
        /** Key for the LLM client instance. */
        public static final ContextKey<LlmClient> LLM_CLIENT = new ContextKey<>("llmClient", LlmClient.class);
        /** Key for the current session identifier. */
        public static final ContextKey<AgentRun> AGENT_RUN_CONTEXT_KEY = new ContextKey<>("AgentRun", AgentRun.class);
    }

    public static final class WallpaperPipeline {
        /** Key for the generated wallpaper theme. */
        public static final ContextKey<String> GENERATED_THEME = new ContextKey<>("generatedTheme", String.class);
        /** Key for the generated wallpaper code. */
        public static final ContextKey<String> GENERATED_CODE = new ContextKey<>("generatedCode", String.class);
    }
}
