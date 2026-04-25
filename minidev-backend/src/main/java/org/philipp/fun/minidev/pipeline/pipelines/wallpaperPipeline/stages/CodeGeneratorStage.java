package org.philipp.fun.minidev.pipeline.pipelines.wallpaperPipeline.stages;

import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.dto.llm.LlmRequest;
import org.philipp.fun.minidev.dto.llm.LlmResponse;
import org.philipp.fun.minidev.pipeline.abstracts.AbstractStep;
import org.philipp.fun.minidev.pipeline.core.ContextKeys;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;

import org.philipp.fun.minidev.dto.llm.JsonSchema;
import org.philipp.fun.minidev.dto.llm.LlmModel;
import java.util.Map;
import java.util.List;
import java.util.Random;

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
                "description", Map.of("type", "string", "description", "Brief description of the animation"),
                "technical_details", Map.of("type", "object", "properties", Map.of(
                    "performance_tricks", Map.of("type", "array", "items", Map.of("type", "string")),
                    "visual_techniques", Map.of("type", "array", "items", Map.of("type", "string"))
                ))
            ),
            "required", List.of("html", "css", "js")
        ));

        List<LlmRequest.Message> messages = List.of(
                LlmRequest.Message.system("""
        You are a senior frontend developer and creative coder specialized in minimalistic animated wallpapers.

        Your task is to implement a web-based live wallpaper from a given theme.

        STRICT STYLE REQUIREMENTS:
        - The wallpaper must work well as a background: visually interesting but not distracting.
        - Motion must be subtle, smooth, slow, and loop-friendly.
        - Use a coherent color palette with muted, sophisticated colors.
        - DARK MODE PREFERENCE: Use dark backgrounds (black, deep blues, charcoal or pastel colors etc.) to ensure the wallpaper is not blinding. Avoid pure white backgrounds.
        - ORGANIC MOTION: Prefer organic, natural motion (swaying, drifting, gentle breathing) over mechanical or perfectly geometric movement.
        - STYLIZED ELEMENTS: Feel free to use stylized shapes or artistic representations (like a single drifting feather or swaying grass) instead of just particles.

        TECHNICAL REQUIREMENTS:
        1. Use HTML5 <canvas> for all animations.
        2. Code MUST be fully self-contained: HTML, CSS, and JavaScript in one result.
        3. Do NOT use external libraries, assets, fonts, images, SVG files, or network requests.
        4. Use requestAnimationFrame for smooth animation.
        5. Implement high-DPI / Retina support by scaling the canvas context.
        6. The animation MUST be responsive and handle window resize correctly.
        7. Canvas must cover the entire viewport with no scrollbars.
        8. Use modern ES6+ JavaScript.
        10. Avoid excessive particle counts or expensive per-frame calculations.
        11. The Walloaoer must be able to scale to any size and resolution.
        

        ANIMATION REQUIREMENTS:
        - The animation should feel continuous and naturally loopable.
        - Use slow interpolation, sinusoidal movement, easing, noise-like motion, or cyclic motion.
        - Mouse interaction is optional and must stay subtle.
        - Interaction must never break the calm wallpaper feeling.

        CODE QUALITY REQUIREMENTS:
        - Produce clean, readable, maintainable code.
        - Use descriptive variable and function names.
        - Avoid unnecessary abstractions.
        - Include only concise comments where helpful.

        OUTPUT REQUIREMENTS:
        - The output MUST be a valid JSON object matching the provided schema.
        - Do NOT include markdown.
        - Do NOT include explanations outside the JSON.
        """
                ),
                LlmRequest.Message.user("""
        Implement a minimalistic animated live wallpaper based on this theme:

        %s

        Interpret the theme creatively, but keep it abstract, calm, and suitable as a desktop/browser background.
        The final wallpaper should feel polished, smooth, and aesthetically pleasing.
        """.formatted(theme))
        );

        LlmRequest request = new LlmRequest(messages, null, null, schema, null, null);

        LlmResponse initialResponse = llmClient.chat(request);
        if (!initialResponse.success()) {
            return false;
        }

        String initialRawJson = initialResponse.content();

        // Step 2: Refinement Step
        List<LlmRequest.Message> refinementMessages = List.of(
                LlmRequest.Message.system("You are an expert creative coder and code reviewer. " +
                        "Your task is to refine and improve the provided HTML5 Canvas wallpaper code. " +
                        "CRITICAL REQUIREMENTS: " +
                        "1. Performance optimization (use Typed Arrays for particle data, minimize canvas state changes). " +
                        "2. Visual polish (smooth gradients, easing functions, bloom effects). " +
                        "3. Code quality (ES6+ features, modular structure, helpful comments). " +
                        "4. Robustness (graceful resize handling, high-DPI/Retina display support). " +
                        "The output MUST be a valid JSON object matching the provided schema."),
                LlmRequest.Message.user("Refine this wallpaper code for the theme '" + theme + "':\n\n" + initialRawJson)
        );

        LlmRequest refinementRequest = new LlmRequest(refinementMessages, null, null, schema, null, null);
        LlmResponse refinedResponse = llmClient.chat(refinementRequest);

        if (!refinedResponse.success()) {
            context.putValue(ContextKeys.WallpaperPipeline.GENERATED_CODE, initialRawJson);
            return true;
        }

        try {
            context.putValue(ContextKeys.WallpaperPipeline.GENERATED_CODE, refinedResponse.content());
        } catch (Exception e) {
            context.putValue(ContextKeys.WallpaperPipeline.GENERATED_CODE, initialRawJson);
        }

        return true;
    }
}
