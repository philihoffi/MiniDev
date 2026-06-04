package org.philipp.fun.minidev.pipeline;

import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.model.AgentRun;

public class ContextKeys {
    public static final class System {
        public static final ContextKey<LlmClient> LLM_CLIENT = new ContextKey<>("llmClient", LlmClient.class);
        public static final ContextKey<AgentRun> AGENT_RUN = new ContextKey<>("AgentRun", AgentRun.class);
    }

    public static final class Wallpaper {
        public static final ContextKey<String> THEME = new ContextKey<>("theme", String.class);
        public static final ContextKey<String> CODE = new ContextKey<>("code", String.class);
    }
}
