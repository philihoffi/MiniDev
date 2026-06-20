package org.philipp.fun.minidev.pipeline.wallpaper.stages;

import org.philipp.fun.minidev.model.Wallpaper;
import org.philipp.fun.minidev.pipeline.annotation.PipelineStage;
import org.philipp.fun.minidev.pipeline.core.BaseElement;
import org.philipp.fun.minidev.pipeline.core.ContextKeys;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.repository.WallpaperRepository;

/**
 * Pipeline stage that persists generated wallpaper data to the database.
 */
@PipelineStage("wallpaper-cache")
public class WallpaperCacheStage extends BaseElement {

    /** Repository used to persist wallpaper entities. */
    private final WallpaperRepository repository;

    /**
     * Constructs a new {@code WallpaperCacheStage}.
     *
     * @param repository the wallpaper repository
     */
    public WallpaperCacheStage(WallpaperRepository repository) {
        super("WallpaperCacheStage");
        this.repository = repository;
    }

    /**
     * Reads the wallpaper code and theme from the pipeline context and persists
     * them as a new {@link Wallpaper} entity.
     *
     * @param context the current pipeline context
     * @return {@code true} on success
     * @throws Exception if persistence fails
     */
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