package org.philipp.fun.minidev.pipeline.wallpaper.stages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.philipp.fun.minidev.pipeline.core.BaseElement;
import org.philipp.fun.minidev.pipeline.core.ContextKey;
import org.philipp.fun.minidev.pipeline.core.ContextKeys;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.annotation.PipelineStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@PipelineStage("code-review-evaluator")
public class CodeReviewEvaluatorStage extends BaseElement {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(CodeReviewEvaluatorStage.class);
    private static final int MIN_PASS_SCORE = 6;

    public CodeReviewEvaluatorStage() {
        super("CodeReviewEvaluatorStage");
    }

    @Override
    public boolean execute(PipelineContext context) throws Exception {
        String rawResult = context.getValue(new ContextKey<>("reviewRawResult", String.class));
        if (rawResult == null || rawResult.isBlank()) {
            log.warn("No review raw result found in context, defaulting to pass");
            context.putValue(ContextKeys.Wallpaper.REVIEW_PASSED, true);
            return true;
        }

        try {
            ReviewResult result = OBJECT_MAPPER.readValue(rawResult, ReviewResult.class);

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

            context.putValue(ContextKeys.Wallpaper.REVIEW_FEEDBACK, feedback.toString());
            context.putValue(ContextKeys.Wallpaper.REVIEW_PASSED, passes);

            log.info("Code review evaluated: overall={}/10, passes={}", result.overall_score(), passes);
            return true;
        } catch (Exception e) {
            log.error("Failed to parse review result", e);
            return false;
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