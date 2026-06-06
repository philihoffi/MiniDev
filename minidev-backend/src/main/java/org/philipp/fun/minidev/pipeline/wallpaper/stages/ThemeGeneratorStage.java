package org.philipp.fun.minidev.pipeline.wallpaper.stages;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.philipp.fun.minidev.dto.CreativeBrief;
import org.philipp.fun.minidev.dto.llm.JsonSchema;
import org.philipp.fun.minidev.dto.llm.LlmRequest;
import org.philipp.fun.minidev.dto.llm.LlmResponse;
import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.pipeline.core.BaseElement;
import org.philipp.fun.minidev.pipeline.core.ContextKeys;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.annotation.PipelineStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.philipp.fun.minidev.pipeline.core.ContextKeys.System.LLM_CLIENT;

@PipelineStage("theme-generator")
public class ThemeGeneratorStage extends BaseElement {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(ThemeGeneratorStage.class);

    public ThemeGeneratorStage() {
        super("ThemeGeneratorStage");
    }

    @Override
    public boolean execute(PipelineContext context) throws Exception {
        LlmClient llmClient = context.getValue(LLM_CLIENT);

        JsonSchema schema = JsonSchema.defaultSchema(Map.of(
                "type", "object",
                "properties", Map.of(
                        "theme", Map.of(
                                "type", "string",
                                "description", "A short, evocative theme name (max 60 chars)"
                        ),
                        "description", Map.of(
                                "type", "string",
                                "description", "A detailed 2-3 sentence description of the visual concept"
                        ),
                        "color_palette", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string"),
                                "description", "3-5 hex color codes that define the palette (e.g. '#1a1a2e', '#16213e', '#0f3460', '#e94560')"
                        ),
                        "mood", Map.of(
                                "type", "string",
                                "description", "One-word mood descriptor (e.g. 'serene', 'mysterious', 'ethereal', 'warm', 'melancholic')"
                        ),
                        "motion_style", Map.of(
                                "type", "string",
                                "description", "How elements should move (e.g. 'slow drifting with gentle parallax', 'rhythmic breathing pulse', 'organic flowing streams')"
                        ),
                        "key_elements", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string"),
                                "description", "2-4 specific visual elements to animate (e.g. 'translucent overlapping circles', 'floating dust particles', 'aurora-like waves')"
                        )
                ),
                "required", List.of("theme", "description", "color_palette", "mood", "motion_style", "key_elements"),
                "additionalProperties", false
        ));

        List<LlmRequest.Message> messages = List.of(
                LlmRequest.Message.system("""
                        You are a visual creative director specializing in animated wallpapers and ambient digital art.

                        Your task: Generate a detailed creative brief for an animated HTML5 Canvas wallpaper.

                        DESIGN PHILOSOPHY:
                        - Subtlety is paramount — the wallpaper should feel alive but never demanding of attention
                        - Every element must justify its presence — no decorative noise
                        - Think "living painting" not "screensaver"
                        - Favor asymmetry and organic imperfection over rigid geometry

                        COLOR RULES:
                        - Always use dark backgrounds (deep navy, charcoal, dark forest, midnight blue)
                        - Palette must have strong contrast between background and accent colors
                        - Use desaturated, muted tones — avoid pure/saturated colors
                        - Include one subtle warm accent against a cool base (or vice versa)

                        MOTION RULES:
                        - Motion must be almost imperceptible at first glance
                        - Use easing curves (cubic-bezier, sinusoidal) — never linear motion
                        - Prefer drift, sway, breathing, and gentle parallax over translation
                        - Elements should feel like they have weight and inertia

                        OUTPUT: A structured creative brief that a developer can directly implement."""),
                LlmRequest.Message.user(
                        "Generate a unique, visually striking creative brief for an animated wallpaper. Surprise me with an unexpected concept."
                )
        );

        LlmRequest request = new LlmRequest(messages, 1.2, null, schema, null, null);

        LlmResponse response = llmClient.chat(request);

        if (!response.success()) {
            log.warn("Theme generation failed: {}", response.errorMessage());
            return false;
        }

        try {
            CreativeBrief brief = OBJECT_MAPPER.readValue(response.content(), CreativeBrief.class);
            context.putValue(ContextKeys.Wallpaper.CREATIVE_BRIEF, brief);
            context.putValue(ContextKeys.Wallpaper.THEME, brief.theme());
            log.info("Generated creative brief: theme='{}', mood='{}', palette={} colors",
                    brief.theme(), brief.mood(), brief.colorPalette().size());
            return true;
        } catch (Exception e) {
            log.error("Failed to parse creative brief", e);
            return false;
        }
    }
}
