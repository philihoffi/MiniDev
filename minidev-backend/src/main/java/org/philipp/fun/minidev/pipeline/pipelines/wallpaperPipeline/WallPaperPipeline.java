package org.philipp.fun.minidev.pipeline.pipelines.wallpaperPipeline;

import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.impl.SequenzStage;
import org.philipp.fun.minidev.pipeline.pipelines.wallpaperPipeline.stages.CodeGeneratorStage;
import org.philipp.fun.minidev.pipeline.pipelines.wallpaperPipeline.stages.ThemeGeneratorStage;
import org.philipp.fun.minidev.pipeline.pipelines.wallpaperPipeline.stages.ValidateCodeStage;
import org.philipp.fun.minidev.pipeline.pipelines.wallpaperPipeline.stages.WallpaperCacheStage;
import org.philipp.fun.minidev.repository.WallpaperRepository;
import org.springframework.stereotype.Component;

import static org.philipp.fun.minidev.pipeline.core.ContextKeys.System.LLM_CLIENT;

@Component
public class WallPaperPipeline extends SequenzStage {
    private final LlmClient llmClient;

    public WallPaperPipeline(LlmClient llmClient, WallpaperRepository repository) {
        super("WallPaperPipeline");
        this.llmClient = llmClient;

        addElement(new WallpaperCacheStage(repository));
        addElement(new ThemeGeneratorStage());
        addElement(new CodeGeneratorStage());
        addElement(new ValidateCodeStage());
        addElement(new WallpaperCacheStage(repository));
    }

    @Override
    public boolean execute(PipelineContext context) {
        context.putValue(LLM_CLIENT, llmClient);
        return super.execute(context);
    }
}
