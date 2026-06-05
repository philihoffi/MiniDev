package org.philipp.fun.minidev.pipeline.wallpaper.stages;

import org.philipp.fun.minidev.dto.CreativeBrief;
import org.philipp.fun.minidev.dto.llm.JsonSchema;
import org.philipp.fun.minidev.dto.llm.LlmRequest;
import org.philipp.fun.minidev.dto.llm.LlmResponse;
import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.pipeline.BaseElement;
import org.philipp.fun.minidev.pipeline.ContextKeys;
import org.philipp.fun.minidev.pipeline.PipelineContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static org.philipp.fun.minidev.pipeline.ContextKeys.System.LLM_CLIENT;

@Component
public class CodeRefinementStage extends BaseElement {
    private static final Logger log = LoggerFactory.getLogger(CodeRefinementStage.class);

    public CodeRefinementStage() {
        super("CodeRefinementStage");
    }

    @Override
    public boolean execute(PipelineContext context) throws Exception {
        LlmClient llmClient = context.getValue(LLM_CLIENT);
        String existingCode = context.getValue(ContextKeys.Wallpaper.CODE);
        CreativeBrief brief = context.getValue(ContextKeys.Wallpaper.CREATIVE_BRIEF);
        String reviewFeedback = context.getValue(ContextKeys.Wallpaper.REVIEW_FEEDBACK);

        if (existingCode == null || reviewFeedback == null) {
            log.warn("Refinement skipped: missing code or review feedback");
            return true;
        }

        String briefText = formatBrief(brief);

        JsonSchema schema = JsonSchema.defaultSchema(Map.of(
            "type", "object",
            "properties", Map.of(
                "html", Map.of("type", "string", "description", "The HTML body content"),
                "css", Map.of("type", "string", "description", "CSS styles"),
                "js", Map.of("type", "string", "description", "Refined JavaScript animation code")
            ),
            "required", List.of("html", "css", "js")
        ));

        List<LlmRequest.Message> messages = List.of(
            LlmRequest.Message.system("""
                    You are an expert creative developer refining an animated wallpaper.

                    Your task: Improve the existing code based on the review feedback. Do NOT rewrite from scratch — surgically improve what exists.

                    REFINEMENT APPROACH:
                    1. Read the review feedback carefully
                    2. Preserve all identified strengths
                    3. Fix every issue mentioned
                    4. Apply the improvement suggestions

                    COMMON IMPROVEMENTS:
                    - LOW VISUAL QUALITY: Add layered gradients (radialGradient), alpha transparency, parallax depth, subtle noise texture
                    - LOW ANIMATION QUALITY: Replace linear motion with easing functions, add secondary micro-motion, adjust timing curves
                    - LOW CODE QUALITY: Extract reusable functions, use meaningful names, pre-calculate constants, minimize per-frame allocations
                    - LOW THEME ADHERENCE: Re-align colors to the palette, ensure mood is reflected in speed/timing, add missing visual elements

                    EASING FUNCTIONS (use these, not linear):
                    - Smoothstep: t => t * t * (3 - 2 * t)
                    - Ease-in-out: t => t < 0.5 ? 2*t*t : -1+(4-2*t)*t
                    - Ease-out-back: t => { const c = 1.70158; return 1 + (c + 1) * Math.pow(t - 1, 3) + c * Math.pow(t - 1, 2); }

                    OUTPUT: Valid JSON matching the schema. NO markdown."""),
            LlmRequest.Message.user("""
                    REFINEMENT REQUEST

                    === CREATIVE BRIEF ===
                    %s

                    === REVIEW FEEDBACK ===
                    %s

                    === CURRENT CODE TO REFINE ===
                    %s

                    Improve this code. Keep its strengths, fix its weaknesses, elevate the overall quality.""".formatted(
                            briefText, reviewFeedback, existingCode))
        );

        LlmRequest request = new LlmRequest(messages, 0.7, null, schema, null, null);
        LlmResponse response = llmClient.chat(request);

        if (!response.success()) {
            log.warn("Refinement failed, retrying full generation: {}", response.errorMessage());
            return false;
        }

        context.putValue(ContextKeys.Wallpaper.CODE, response.content());
        context.putValue(ContextKeys.Wallpaper.REVIEW_FEEDBACK, null);
        log.info("Code refined ({} chars)", response.content().length());
        return true;
    }

    private String formatBrief(CreativeBrief brief) {
        if (brief == null) {
            return "No brief available.";
        }
        return brief.format();
    }
}
