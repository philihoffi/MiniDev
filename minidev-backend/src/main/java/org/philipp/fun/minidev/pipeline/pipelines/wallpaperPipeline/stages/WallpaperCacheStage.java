package org.philipp.fun.minidev.pipeline.pipelines.wallpaperPipeline.stages;

import org.philipp.fun.minidev.model.Wallpaper;
import org.philipp.fun.minidev.pipeline.BaseElement;
import org.philipp.fun.minidev.pipeline.ContextKeys;
import org.philipp.fun.minidev.pipeline.PipelineContext;
import org.philipp.fun.minidev.repository.WallpaperRepository;
import org.springframework.stereotype.Component;

@Component
public class WallpaperCacheStage extends BaseElement {
    private final WallpaperRepository repository;

    public WallpaperCacheStage(WallpaperRepository repository) {
        super("WallpaperCacheStage");
        this.repository = repository;
    }

    @Override
    public boolean execute(PipelineContext context) throws Exception {
        String code = context.getValue(ContextKeys.Wallpaper.CODE);
        String theme = context.getValue(ContextKeys.Wallpaper.THEME);

        if (code != null && theme != null) {
            Wallpaper wallpaper = new Wallpaper();
            wallpaper.setCode(code);
            wallpaper.setTheme(theme);
            repository.save(wallpaper);
        }

        return true;
    }
}
