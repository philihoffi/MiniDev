package org.philipp.fun.minidev.pipeline.pipelines.wallpaperPipeline.stages;

import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.llm.objects.LlmRequest;
import org.philipp.fun.minidev.llm.objects.LlmResponse;
import org.philipp.fun.minidev.pipeline.abstracts.AbstractStep;
import org.philipp.fun.minidev.pipeline.core.ContextKeys;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;

import org.philipp.fun.minidev.llm.objects.JsonSchema;
import java.util.Map;
import java.util.List;
import static org.philipp.fun.minidev.pipeline.core.ContextKeys.System.LLM_CLIENT;

public class CodeGeneratorStage extends AbstractStep {
    public CodeGeneratorStage() {
        super("CodeGeneratorStage");
    }

    @Override
    public boolean execute(PipelineContext context) throws Exception {
        LlmClient llmClient = context.getValue(LLM_CLIENT);
        String theme = context.getValue(ContextKeys.WallpaperPipeline.GENERATED_THEME);

        JsonSchema schema = JsonSchema.defaultSchema(Map.of(
            "type", "object",
            "properties", Map.of(
                "html", Map.of("type", "string", "description", "The body content or canvas element"),
                "css", Map.of("type", "string", "description", "The CSS for styling"),
                "js", Map.of("type", "string", "description", "The JavaScript code for the animation"),
                "description", Map.of("type", "string", "description", "Brief description of the animation")
            ),
            "required", List.of("html", "css", "js")
        ));

        List<LlmRequest.Message> messages = List.of(
                LlmRequest.Message.system("You are a senior frontend developer and creative coder. " +
                        "Your goal is to create a high-performance, visually stunning, and smooth animation for a web-based live wallpaper. " +
                        "GUIDELINES: " +
                        "1. Use HTML5 <canvas> for all animations. " +
                        "2. The animation must be responsive (handle window resize). " +
                        "3. Colors should be modern, sophisticated, and suitable for a background (not too bright or distracting). " +
                        "4. Code MUST be self-contained (no external libraries). " +
                        "5. Use requestAnimationFrame for smooth 60fps movement. " +
                        "6. Implement clean, readable, and optimized JavaScript code. " +
                        "7. Ensure the CSS makes the canvas cover the full background without scrollbars. " +
                        "The output MUST be a valid JSON object matching the provided schema."),
                LlmRequest.Message.user("Create a 'Live Wallpaper' for the theme: '" + theme + "'. " +
                        "Include subtle interactions (e.g., mouse parallax or hover effects) if appropriate for the theme. " +
                        "Ensure the animation is loopable and aesthetically pleasing.")
        );

        LlmRequest request = new LlmRequest(messages, schema);

        LlmResponse response = llmClient.chat(request);

        if (!response.success()) {
            return false;
        }

        context.put(ContextKeys.WallpaperPipeline.GENERATED_CODE, response.content());

        return true;
    }
}
