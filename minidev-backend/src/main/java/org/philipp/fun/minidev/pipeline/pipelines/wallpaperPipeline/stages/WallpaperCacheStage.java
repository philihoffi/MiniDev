package org.philipp.fun.minidev.pipeline.pipelines.wallpaperPipeline.stages;

import org.philipp.fun.minidev.pipeline.abstracts.AbstractStep;
import org.philipp.fun.minidev.pipeline.core.ContextKeys;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.spring.model.Wallpaper;
import org.philipp.fun.minidev.spring.repository.WallpaperRepository;

public class WallpaperCacheStage extends AbstractStep {
    private final WallpaperRepository repository;

    public WallpaperCacheStage(WallpaperRepository repository) {
        super("WallpaperCacheStage");
        this.repository = repository;
    }

    @Override
    public boolean execute(PipelineContext context) throws Exception {
        String code = context.getValue(ContextKeys.WallpaperPipeline.GENERATED_CODE);
        String theme = context.getValue(ContextKeys.WallpaperPipeline.GENERATED_THEME);

        if (code != null && theme != null) {
            Wallpaper wallpaper = new Wallpaper();
            wallpaper.setCode(code);
            wallpaper.setTheme(theme);
            repository.save(wallpaper);
        }

        return true;
    }
}
