package org.philipp.fun.minidev.services;

import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.pipelines.wallpaperPipeline.WallPaperPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WallpaperGenerationService {

    private static final Logger log = LoggerFactory.getLogger(WallpaperGenerationService.class);

    private final WallPaperPipeline wallPaperPipeline;

    public WallpaperGenerationService(WallPaperPipeline wallPaperPipeline) {
        this.wallPaperPipeline = wallPaperPipeline;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean generateNewWallpaperInNewTransaction() {
        PipelineContext context = new PipelineContext();
        try {
            boolean success = wallPaperPipeline.execute(context);
            if (!success) {
                log.error("Failed to generate wallpaper: Pipeline execution returned false");
            }
            return success;
        } catch (Exception e) {
            log.error("Error during wallpaper generation: {}", e.getMessage(), e);
            return false;
        }
    }
}

