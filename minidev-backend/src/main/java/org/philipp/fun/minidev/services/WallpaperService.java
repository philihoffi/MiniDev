package org.philipp.fun.minidev.services;

import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.pipelines.wallpaperPipeline.WallPaperPipeline;
import org.philipp.fun.minidev.spring.model.Wallpaper;
import org.philipp.fun.minidev.spring.repository.WallpaperRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class WallpaperService {

    private static final Logger log = LoggerFactory.getLogger(WallpaperService.class);

    private final WallpaperRepository wallpaperRepository;
    private final WallPaperPipeline wallPaperPipeline;

    public WallpaperService(WallpaperRepository wallpaperRepository, WallPaperPipeline wallPaperPipeline) {
        this.wallpaperRepository = wallpaperRepository;
        this.wallPaperPipeline = wallPaperPipeline;
    }

    public Optional<Wallpaper> getRandomWallpaper() {
        checkAndGenerateIfEmpty();
        return wallpaperRepository.findRandomWallpaper();
    }

    private void checkAndGenerateIfEmpty() {
        if (wallpaperRepository.count() == 0) {
            log.info("No wallpapers found in DB. Generating a new one.");
            generateNewWallpaper();
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void generateDailyWallpaper() {
        log.info("Generating daily wallpaper.");
        generateNewWallpaper();
    }

    public void generateNewWallpaper() {
        PipelineContext context = new PipelineContext();
        try {
            boolean success = wallPaperPipeline.execute(context);
            if (!success) {
                log.error("Failed to generate wallpaper");
            }
        } catch (Exception e) {
            log.error("Error during wallpaper generation", e);
        }
    }
}
