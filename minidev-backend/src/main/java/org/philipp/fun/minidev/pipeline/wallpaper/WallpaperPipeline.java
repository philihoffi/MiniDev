package org.philipp.fun.minidev.pipeline.wallpaper;

import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.pipeline.Conditional;
import org.philipp.fun.minidev.pipeline.PipelineContext;
import org.philipp.fun.minidev.pipeline.Retry;
import org.philipp.fun.minidev.pipeline.Sequence;
import org.philipp.fun.minidev.pipeline.wallpaper.stages.AssembleHtmlStage;
import org.philipp.fun.minidev.pipeline.wallpaper.stages.CodeGeneratorStage;
import org.philipp.fun.minidev.pipeline.wallpaper.stages.CodeRefinementStage;
import org.philipp.fun.minidev.pipeline.wallpaper.stages.CodeReviewStage;
import org.philipp.fun.minidev.pipeline.wallpaper.stages.ThemeGeneratorStage;
import org.philipp.fun.minidev.pipeline.wallpaper.stages.WallpaperCacheStage;
import org.springframework.stereotype.Component;

import static org.philipp.fun.minidev.pipeline.ContextKeys.System.LLM_CLIENT;
import static org.philipp.fun.minidev.pipeline.ContextKeys.Wallpaper.REVIEW_PASSED;

@Component
public class WallpaperPipeline extends Sequence {
    private final LlmClient llmClient;

    public WallpaperPipeline(
            LlmClient llmClient,
            ThemeGeneratorStage themeGeneratorStage,
            CodeGeneratorStage codeGeneratorStage,
            CodeReviewStage codeReviewStage,
            CodeRefinementStage codeRefinementStage,
            AssembleHtmlStage assembleHtmlStage,
            WallpaperCacheStage wallpaperCacheStage
    ) {
        super("WallpaperPipeline");
        this.llmClient = llmClient;

        // Phase 1: Generate + Review + Refine (with retry on failure)
        Retry generationRetry = new Retry("GenerationRetry", 3);
        generationRetry.add(themeGeneratorStage);
        generationRetry.add(codeGeneratorStage);
        generationRetry.add(codeReviewStage);
        generationRetry.add(new Conditional("QualityGate",
                ctx -> Boolean.TRUE.equals(ctx.getValue(REVIEW_PASSED)),
                new Sequence("Approved"),
                codeRefinementStage));

        add(generationRetry);
        add(assembleHtmlStage);
        add(wallpaperCacheStage);
    }

    @Override
    public boolean execute(PipelineContext context) {
        context.putValue(LLM_CLIENT, llmClient);
        return super.execute(context);
    }
}
