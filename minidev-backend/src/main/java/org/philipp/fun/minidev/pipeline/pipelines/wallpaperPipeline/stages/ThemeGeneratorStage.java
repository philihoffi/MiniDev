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

        String model = selectRandomModel(llmClient);

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
                        You are an expert generative artist and motion designer for HTML5 Canvas wallpapers.
                        
                        Your task is to generate ONE original wallpaper theme.
                        
                        GOAL:
                        - Produce a theme that is visually distinct and not a variation of common tropes
                        - Avoid overusing neon, cyberpunk, particles, waves, grids, galaxies, fractals, and glowing lines unless explicitly necessary
                        - Prefer unusual but still visually plausible concepts
                        - Explore different artistic directions across repeated generations
                        
                        REQUIREMENTS:
                        - The theme must imply motion, atmosphere, and a clear visual identity
                        - The theme should be suitable for an animated HTML5 Canvas wallpaper
                        - Make it modern, aesthetic, and implementation-friendly
                        
                        OUTPUT:
                        Return only the theme text."""),
                LlmRequest.Message.user(
                        "Generate a unique animated wallpaper theme with a fresh visual direction."
                )
        );

        LlmRequest request = new LlmRequest(messages, 1.0, null, schema, null, model);

        LlmResponse response = llmClient.chat(request);

        if (!response.success() && model != null) {
            log.warn("Random model {} failed, falling back to default model", model);
            request = new LlmRequest(messages, 1.0, null, schema, null, null);
            response = llmClient.chat(request);
        }

        if (!response.success()) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> content = response.getContentAs(Map.class);
        context.putValue(ContextKeys.WallpaperPipeline.GENERATED_THEME, (String) content.get("theme"));

        return true;
    }

    private String selectRandomModel(LlmClient llmClient) {
        List<LlmModel> models = llmClient.getModels();
        if (models == null || models.isEmpty()) {
            return null;
        }

        // Filter models suitable for structured output/chat
        List<String> suitableModels = models.stream()
                .filter(m -> {
                    // Priority 1: Explicitly supports structured outputs
                    if (Boolean.TRUE.equals(m.structuredOutputs())) {
                        return true;
                    }
                    // Priority 2: Supports response_format parameter (but exclude known incompatible models)
                    if (m.supportedParameters() != null && m.supportedParameters().contains("response_format")) {
                        // Exclude older OpenAI models that only support json_object, not json_schema
                        return (!m.id().startsWith("openai/gpt-4") || m.id().contains("gpt-4o") || m.id().contains("gpt-4-turbo")) && !m.id().equals("openai/gpt-3.5-turbo");
                    }
                    return false;
                })
                .filter(m -> m.architecture() != null &&
                        m.architecture().inputModalities() != null && m.architecture().inputModalities().contains("text") &&
                        m.architecture().outputModalities() != null && m.architecture().outputModalities().contains("text"))
                .map(LlmModel::id)
                .toList();

        if (suitableModels.isEmpty()) {
            return null;
        }

        return suitableModels.get(random.nextInt(suitableModels.size()));
    }
}
