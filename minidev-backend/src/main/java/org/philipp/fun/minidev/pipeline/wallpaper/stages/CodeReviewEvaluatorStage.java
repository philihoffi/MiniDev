package org.philipp.fun.minidev.pipeline.wallpaper.stages;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.philipp.fun.minidev.pipeline.annotation.PipelineStage;
import org.philipp.fun.minidev.pipeline.core.BaseElement;
import org.philipp.fun.minidev.pipeline.core.ContextKey;
import org.philipp.fun.minidev.pipeline.core.ContextKeys;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pipeline stage that evaluates code review results against quality thresholds.
 */
@PipelineStage("code-review-evaluator")
public class CodeReviewEvaluatorStage extends BaseElement {

    /** Object mapper for JSON deserialization. */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(CodeReviewEvaluatorStage.class);

    /** Minimum overall score to pass review. */
    private static final int MIN_PASS_SCORE = 6;

    /**
     * Constructs a CodeReviewEvaluatorStage.
     */
    public CodeReviewEvaluatorStage() {
        super("CodeReviewEvaluatorStage");
    }

    /**
     * Executes the code review evaluation.
     *
     * @param context the pipeline context
     * @return true if execution succeeded, false otherwise
     * @throws Exception if an error occurs
     */
    @Override
    public boolean execute(PipelineContext context) throws Exception {
        String rawResult = context.getValue(
                new ContextKey<>("reviewRawResult", String.class));
        if (rawResult == null || rawResult.isBlank()) {
            LOG.warn("No review raw result found in context");
            return false;
        }

        try {
            ReviewResult result = OBJECT_MAPPER.readValue(rawResult, ReviewResult.class);

            boolean passes = result.overall_score() >= MIN_PASS_SCORE
                    && result.visual_quality() >= 4
                    && result.animation_quality() >= 4
                    && result.code_quality() >= 4
                    && result.theme_adherence() >= 4;

            StringBuilder feedback = new StringBuilder();
            feedback.append(String.format(
                    "Overall: %d/10 | Visual: %d | Animation: %d | Code: %d | Theme: %d\n",
                    result.overall_score(), result.visual_quality(), result.animation_quality(),
                    result.code_quality(), result.theme_adherence()));

            if (!result.strengths().isEmpty()) {
                feedback.append("Strengths: ")
                        .append(String.join("; ", result.strengths())).append("\n");
            }
            if (!result.issues().isEmpty()) {
                feedback.append("Issues: ")
                        .append(String.join("; ", result.issues())).append("\n");
            }
            if (!result.improvement_suggestions().isEmpty()) {
                feedback.append("Suggestions: ")
                        .append(String.join("; ", result.improvement_suggestions()));
            }

            context.putValue(ContextKeys.Wallpaper.REVIEW_FEEDBACK, feedback.toString());
            context.putValue(ContextKeys.Wallpaper.REVIEW_PASSED, passes);

            LOG.info("Code review evaluated: overall={}/10, passes={}",
                    result.overall_score(), passes);
            return true;
        } catch (Exception e) {
            LOG.error("Failed to parse review result", e);
            return false;
        }
    }

    /**
     * Record representing a code review result.
     *
     * @param visual_quality          the visual quality score
     * @param animation_quality       the animation quality score
     * @param code_quality            the code quality score
     * @param theme_adherence         the theme adherence score
     * @param overall_score           the overall score
     * @param passes                  whether the review passes
     * @param strengths               the list of strengths
     * @param issues                  the list of issues
     * @param improvement_suggestions the list of improvement suggestions
     */
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
    ) {
    }
}