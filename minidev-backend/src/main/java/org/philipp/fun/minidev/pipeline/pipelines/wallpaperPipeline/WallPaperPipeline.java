package org.philipp.fun.minidev.pipeline.pipelines.wallpaperPipeline;

import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.pipeline.PipelineContext;
import org.philipp.fun.minidev.pipeline.Retry;
import org.philipp.fun.minidev.pipeline.Sequence;
import org.philipp.fun.minidev.pipeline.pipelines.wallpaperPipeline.stages.CodeGeneratorStage;
import org.philipp.fun.minidev.pipeline.pipelines.wallpaperPipeline.stages.ThemeGeneratorStage;
import org.philipp.fun.minidev.pipeline.pipelines.wallpaperPipeline.stages.ValidateCodeStage;
import org.philipp.fun.minidev.pipeline.pipelines.wallpaperPipeline.stages.WallpaperCacheStage;
import org.springframework.stereotype.Component;

import static org.philipp.fun.minidev.pipeline.ContextKeys.System.LLM_CLIENT;

@Component
public class WallPaperPipeline extends Sequence {
    private final LlmClient llmClient;

    public WallPaperPipeline(
            LlmClient llmClient,
            ThemeGeneratorStage themeGeneratorStage,
            CodeGeneratorStage codeGeneratorStage,
            ValidateCodeStage validateCodeStage,
            WallpaperCacheStage wallpaperCacheStage
    ) {
        super("WallPaperPipeline");
        this.llmClient = llmClient;

        add(new Retry("GenerationRetry", 5)
                .add(themeGeneratorStage)
                .add(codeGeneratorStage)
                .add(validateCodeStage));

        add(wallpaperCacheStage);
    }

    @Override
    public boolean execute(PipelineContext context) {
        context.putValue(LLM_CLIENT, llmClient);
        return super.execute(context);
    }
}
