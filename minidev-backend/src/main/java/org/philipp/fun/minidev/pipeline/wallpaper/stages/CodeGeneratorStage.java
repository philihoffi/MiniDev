package org.philipp.fun.minidev.pipeline.wallpaper.stages;

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

@PipelineStage("code-generator")
public class CodeGeneratorStage extends BaseElement {
    private static final Logger log = LoggerFactory.getLogger(CodeGeneratorStage.class);

    public CodeGeneratorStage() {
        super("CodeGeneratorStage");
    }

    @Override
    public boolean execute(PipelineContext context) throws Exception {
        LlmClient llmClient = context.getValue(LLM_CLIENT);
        CreativeBrief brief = context.getValue(ContextKeys.Wallpaper.CREATIVE_BRIEF);
        String theme = context.getValue(ContextKeys.Wallpaper.THEME);

        String briefText = formatBrief(brief, theme);

        JsonSchema schema = JsonSchema.defaultSchema(Map.of(
            "type", "object",
            "properties", Map.of(
                "html", Map.of("type", "string", "description", "The HTML body content (canvas element, minimal markup)"),
                "css", Map.of("type", "string", "description", "CSS styles (minimal, mostly for body/canvas)"),
                "js", Map.of("type", "string", "description", "Complete JavaScript animation code")
            ),
            "required", List.of("html", "css", "js")
        ));

        List<LlmRequest.Message> messages = List.of(
            LlmRequest.Message.system("""
                    You are a senior creative developer specializing in generative art and HTML5 Canvas animations.

                    Your task: Implement a stunning animated wallpaper based on the provided creative brief.

                    VISUAL EXCELLENCE RULES:
                    - Every pixel matters — create visual richness through layered effects
                    - Use alpha transparency and compositing for depth (globalAlpha, 'screen', 'overlay' blend modes)
                    - Add subtle noise/grain texture to prevent flat color blocks
                    - Implement visual depth with parallax layers (foreground, midground, background)
                    - Use gradients (radial and linear) for atmospheric lighting effects
                    - Prefer asymmetric, organic compositions over centered/rigid layouts

                    ANIMATION MASTERY RULES:
                    - All motion MUST use easing functions — never linear interpolation
                        * Smoothstep: t => t * t * (3 - 2 * t)
                        * Ease-in-out: t => t < 0.5 ? 2*t*t : -1+(4-2*t)*t
                        * Sinusoidal: Math.sin(t * Math.PI) for breathing/pulsing
                    - Add subtle secondary motion (slight rotation wobble, scale breathing, opacity shifts)
                    - Use time-based animation (timestamp from requestAnimationFrame), NOT frame-based
                    - Animation cycle duration: 4-15 seconds for main motion, 1-3 seconds for micro-motion
                    - Add slight randomness to element properties for organic feel

                    TECHNICAL REQUIREMENTS:
                    1. HTML5 <canvas> element that fills the entire viewport
                    2. High-DPI/Retina support: scale canvas by window.devicePixelRatio
                    3. Handle window resize gracefully
                    4. Use requestAnimationFrame with timestamp-based delta timing
                    5. Fully self-contained — NO external dependencies, libraries, fonts, or network requests
                    6. Clean, readable code with descriptive variable names
                    7. Canvas context MUST be properly sized for device pixel ratio

                    PERFORMANCE RULES:
                    - Pre-calculate values that don't change per frame
                    - Reuse objects — avoid allocations inside the animation loop
                    - Keep element counts reasonable (< 500 particles/elements)
                    - Minimize canvas state changes (group draws by style)
                    - Use TypedArrays for large datasets

                    OUTPUT: Valid JSON matching the schema. NO markdown, NO explanations."""),
            LlmRequest.Message.user("""
                    Create an animated wallpaper based on this creative brief:

                    %s

                    Bring this vision to life with rich visual detail, smooth organic animation, and polished code.
                    The result should feel like a premium, living wallpaper — subtle, atmospheric, and beautiful.""".formatted(briefText))
        );

        LlmRequest request = new LlmRequest(messages, 0.9, null, schema, null, null);
        LlmResponse response = llmClient.chat(request);

        if (!response.success()) {
            log.warn("Code generation failed: {}", response.errorMessage());
            return false;
        }

        context.putValue(ContextKeys.Wallpaper.CODE, response.content());
        log.info("Code generated ({} chars)", response.content().length());
        return true;
    }

    private String formatBrief(CreativeBrief brief, String theme) {
        if (brief == null) {
            return "Theme: " + (theme != null ? theme : "unknown");
        }
        return brief.format();
    }
}
