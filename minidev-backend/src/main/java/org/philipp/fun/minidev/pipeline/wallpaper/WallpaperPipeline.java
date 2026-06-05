package org.philipp.fun.minidev.pipeline.wallpaper;

import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.pipeline.PipelineContext;
import org.philipp.fun.minidev.pipeline.Retry;
import org.philipp.fun.minidev.pipeline.Sequence;
import org.philipp.fun.minidev.pipeline.wallpaper.stages.CodeGeneratorStage;
import org.philipp.fun.minidev.pipeline.wallpaper.stages.ThemeGeneratorStage;
import org.philipp.fun.minidev.pipeline.wallpaper.stages.ValidateCodeStage;
import org.philipp.fun.minidev.pipeline.wallpaper.stages.WallpaperCacheStage;
import org.springframework.stereotype.Component;

import static org.philipp.fun.minidev.pipeline.ContextKeys.System.LLM_CLIENT;

@Component
public class WallpaperPipeline extends Sequence {
    private final LlmClient llmClient;

    public WallpaperPipeline(
            LlmClient llmClient,
            ThemeGeneratorStage themeGeneratorStage,
            CodeGeneratorStage codeGeneratorStage,
            ValidateCodeStage validateCodeStage,
            WallpaperCacheStage wallpaperCacheStage
    ) {
        super("WallpaperPipeline");
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
