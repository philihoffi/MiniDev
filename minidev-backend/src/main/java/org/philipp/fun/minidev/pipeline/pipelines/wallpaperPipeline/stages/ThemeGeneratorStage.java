package org.philipp.fun.minidev.pipeline.pipelines.wallpaperPipeline.stages;

import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.dto.llm.LlmRequest;
import org.philipp.fun.minidev.dto.llm.LlmResponse;
import org.philipp.fun.minidev.pipeline.abstracts.AbstractStep;
import org.philipp.fun.minidev.pipeline.core.ContextKeys;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;

import java.util.List;

import static org.philipp.fun.minidev.pipeline.core.ContextKeys.System.LLM_CLIENT;

public class ThemeGeneratorStage extends AbstractStep {

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

        List<LlmRequest.Message> messages = List.of(
                LlmRequest.Message.system("You are a creative visual designer. Your task is to generate a unique, highly visual, and inspiring theme for an animated HTML5 Canvas wallpaper. " +
                        "The theme should imply movement, color, and atmosphere (e.g., 'Nebula pulse with deep violet and neon cyan' or 'Geometric grid shift in retro-futuristic style'). " +
                        "Output ONLY the theme name. Be concise (max 12 words). No markdown, no quotes."),
                LlmRequest.Message.user("Generate a creative and modern wallpaper theme.")
        );

        LlmRequest request = new LlmRequest(messages);

        LlmResponse response = llmClient.chat(request);

        context.put(ContextKeys.WallpaperPipeline.GENERATED_THEME, response.content());

        return true;
    }
}
