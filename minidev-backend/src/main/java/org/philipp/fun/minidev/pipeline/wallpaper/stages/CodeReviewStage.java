package org.philipp.fun.minidev.pipeline.wallpaper.stages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.philipp.fun.minidev.dto.CreativeBrief;
import org.philipp.fun.minidev.dto.WallpaperCode;
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

@PipelineStage("code-review")
public class CodeReviewStage extends BaseElement {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(CodeReviewStage.class);
    private static final int MIN_PASS_SCORE = 6;

    public CodeReviewStage() {
        super("CodeReviewStage");
    }

    @Override
    public boolean execute(PipelineContext context) throws Exception {
        LlmClient llmClient = context.getValue(LLM_CLIENT);
        String rawJson = context.getValue(ContextKeys.Wallpaper.CODE);
        CreativeBrief brief = context.getValue(ContextKeys.Wallpaper.CREATIVE_BRIEF);

        if (rawJson == null || rawJson.isBlank()) {
            return false;
        }

        if (llmClient == null) {
            log.debug("No LLM client available, skipping code review");
            context.putValue(ContextKeys.Wallpaper.REVIEW_PASSED, true);
            return true;
        }

        WallpaperCode code;
        try {
            code = OBJECT_MAPPER.readValue(rawJson, WallpaperCode.class);
        } catch (Exception e) {
            log.warn("Failed to parse code for review, retrying: {}", e.getMessage());
            return false;
        }

        String briefContext = brief != null
                ? String.format("Theme: '%s'\nDescription: %s\nMood: %s\nMotion: %s\nColors: %s\nElements: %s",
                    brief.theme(), brief.description(), brief.mood(),
                    brief.motionStyle(), brief.colorPalette(), brief.keyElements())
                : "No creative brief available.";

        JsonSchema schema = JsonSchema.defaultSchema(Map.of(
                "type", "object",
                "properties", Map.of(
                        "visual_quality", Map.of("type", "integer", "description", "Score 1-10: How visually appealing is the described output likely to be"),
                        "animation_quality", Map.of("type", "integer", "description", "Score 1-10: How smooth, natural, and well-crafted is the animation logic"),
                        "code_quality", Map.of("type", "integer", "description", "Score 1-10: How clean, efficient, and well-structured is the JavaScript"),
                        "theme_adherence", Map.of("type", "integer", "description", "Score 1-10: How well does the code match the creative brief"),
                        "overall_score", Map.of("type", "integer", "description", "Score 1-10: Overall quality rating"),
                        "passes", Map.of("type", "boolean", "description", "True if overall_score >= 6 and no dimension is below 4"),
                        "strengths", Map.of("type", "array", "items", Map.of("type", "string"), "description", "2-3 specific things done well"),
                        "issues", Map.of("type", "array", "items", Map.of("type", "string"), "description", "Specific problems found (be precise and actionable)"),
                        "improvement_suggestions", Map.of("type", "array", "items", Map.of("type", "string"), "description", "Concrete, actionable suggestions to elevate the wallpaper")
                ),
                "required", List.of("visual_quality", "animation_quality", "code_quality", "theme_adherence", "overall_score", "passes", "strengths", "issues", "improvement_suggestions")
        ));

        List<LlmRequest.Message> messages = List.of(
                LlmRequest.Message.system("""
                        You are a senior creative developer and visual quality reviewer for animated HTML5 Canvas wallpapers.

                        Your job: Critically review the wallpaper code against the creative brief and provide an honest assessment.

                        REVIEW CRITERIA:

                        VISUAL QUALITY (1-10):
                        - Color usage: Are colors harmonious? Is contrast appropriate?
                        - Composition: Is the visual layout balanced and interesting?
                        - Detail level: Enough visual richness without clutter?
                        - Aesthetic: Does it feel like a polished, premium wallpaper?

                        ANIMATION QUALITY (1-10):
                        - Smoothness: Are animations fluid with proper easing?
                        - Naturalness: Does motion feel organic, not robotic?
                        - Timing: Are animation speeds appropriate (not too fast/slow)?
                        - Loop: Does the animation feel continuous and seamless?

                        CODE QUALITY (1-10):
                        - Performance: Efficient use of canvas API, minimal allocations per frame?
                        - Structure: Clean, readable code with meaningful variable names?
                        - Robustness: Handles resize, high-DPI, edge cases?
                        - Best practices: requestAnimationFrame usage, no memory leaks?

                        THEME ADHERENCE (1-10):
                        - Does the code accurately implement the creative brief?
                        - Are the specified colors, mood, and elements present?
                        - Does the motion match the described style?

                        Be HARSH but CONSTRUCTIVE. A 7+ should mean genuinely good.
                        Identify the WEAKEST aspect and give specific fix instructions."""),
                LlmRequest.Message.user("""
                        REVIEW THIS WALLPAPER CODE:

                        === CREATIVE BRIEF ===
                        %s

                        === HTML ===
                        %s

                        === CSS ===
                        %s

                        === JAVASCRIPT ===
                        %s
                        """.formatted(briefContext,
                            code.html() != null ? code.html() : "(none)",
                            code.css() != null ? code.css() : "(none)",
                            code.js()))
        );

        LlmRequest request = new LlmRequest(messages, 0.3, null, schema, null, null);
        LlmResponse response = llmClient.chat(request);

        if (!response.success()) {
            log.warn("Code review LLM call failed: {}", response.errorMessage());
            context.putValue(ContextKeys.Wallpaper.REVIEW_PASSED, true);
            return true;
        }

        try {
            ReviewResult result = OBJECT_MAPPER.readValue(response.content(), ReviewResult.class);

            boolean passes = result.overall_score() >= MIN_PASS_SCORE
                    && result.visual_quality() >= 4
                    && result.animation_quality() >= 4
                    && result.code_quality() >= 4
                    && result.theme_adherence() >= 4;

            StringBuilder feedback = new StringBuilder();
            feedback.append(String.format("Overall: %d/10 | Visual: %d | Animation: %d | Code: %d | Theme: %d\n",
                    result.overall_score(), result.visual_quality(), result.animation_quality(),
                    result.code_quality(), result.theme_adherence()));

            if (!result.strengths().isEmpty()) {
                feedback.append("Strengths: ").append(String.join("; ", result.strengths())).append("\n");
            }
            if (!result.issues().isEmpty()) {
                feedback.append("Issues: ").append(String.join("; ", result.issues())).append("\n");
            }
            if (!result.improvement_suggestions().isEmpty()) {
                feedback.append("Suggestions: ").append(String.join("; ", result.improvement_suggestions()));
            }

            String feedbackStr = feedback.toString();
            context.putValue(ContextKeys.Wallpaper.REVIEW_FEEDBACK, feedbackStr);
            context.putValue(ContextKeys.Wallpaper.REVIEW_PASSED, passes);

            log.info("Code review: overall={}/10, passes={}", result.overall_score(), passes);
            if (!passes) {
                log.info("Review feedback: {}", feedbackStr);
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to parse review result", e);
            context.putValue(ContextKeys.Wallpaper.REVIEW_PASSED, true);
            return true;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReviewResult(
            int visual_quality,
            int animation_quality,
            int code_quality,
            int theme_adherence,
            int overall_score,
            boolean passes,
            List<String> strengths,
            List<String> issues,
            List<String> improvement_suggestions
    ) {}
}
