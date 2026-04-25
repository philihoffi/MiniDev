package org.philipp.fun.minidev.pipeline.pipelines.wallpaperPipeline.stages;

import org.philipp.fun.minidev.dto.llm.JsonSchema;
import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.dto.llm.LlmModel;
import org.philipp.fun.minidev.dto.llm.LlmRequest;
import org.philipp.fun.minidev.dto.llm.LlmResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.philipp.fun.minidev.pipeline.abstracts.AbstractStep;
import org.philipp.fun.minidev.pipeline.core.ContextKeys;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.philipp.fun.minidev.pipeline.core.ContextKeys.System.LLM_CLIENT;

public class ThemeGeneratorStage extends AbstractStep {

    private static final Logger log = LoggerFactory.getLogger(ThemeGeneratorStage.class);
    private final Random random = new Random();

    public ThemeGeneratorStage() {
        super("ThemeGeneratorStage");
    }

    @Override
    public String getName() {
        return "ThemeGeneratorStage";
    }

    @Override
    public boolean execute(PipelineContext context) throws Exception {
        LlmClient llmClient = context.getValue(LLM_CLIENT);

        JsonSchema schema = JsonSchema.defaultSchema(Map.of(
                "type", "object",
                "properties", Map.of(
                        "theme", Map.of(
                                "type", "string",
                                "description", "A unique and inspiring theme for an animated HTML5 Canvas wallpaper.",
                                "pattern", "^[^\"'`]{1,100}$"
                        )
                ),
                "required", List.of("theme"),
                "additionalProperties", false
        ));

        List<LlmRequest.Message> messages = List.of(
                LlmRequest.Message.system("""
                        You are a creative AI specialized in generating unique, animated wallpaper themes.
                        
                                                    Your task is to generate ONE original wallpaper theme concept.
                        
                                                    STRICT REQUIREMENTS:
                                                    - The animation MUST be subtle, smooth, and loopable
                                                    - The theme MUST feel modern, aesthetic, and calming
                                                    - Use a UNIQUE visual concept every time
                                                    - Prefer natural, organic, and poetic themes (e.g., whispering reeds, drifting feathers, subtle light play)
                                                    - Use muted, sophisticated color palettes (e.g., emerald and ochre, deep indigo and copper, muted gold and charcoal)
                                                    - Prefer DARK and moody color schemes or pastel color schemes for better desktop background suitability.
                                                    - Avoid generic "high-tech" or "neon" styles unless explicitly asked for."""),
                LlmRequest.Message.user(
                        "Generate a unique animated wallpaper theme with a fresh visual direction."
                )
        );

        LlmRequest request = new LlmRequest(messages, 2.0, null, schema, null, null);

        LlmResponse response = llmClient.chat(request);

        if (!response.success()) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> content = response.getContentAs(Map.class);
        context.putValue(ContextKeys.WallpaperPipeline.GENERATED_THEME, (String) content.get("theme"));

        return true;
    }
}
