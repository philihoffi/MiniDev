package org.philipp.fun.minidev.pipeline.wallpaper.stages;

import org.philipp.fun.minidev.model.Wallpaper;
import org.philipp.fun.minidev.pipeline.core.BaseElement;
import org.philipp.fun.minidev.pipeline.core.ContextKeys;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.annotation.PipelineStage;
import org.philipp.fun.minidev.repository.WallpaperRepository;

@PipelineStage("wallpaper-cache")
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
