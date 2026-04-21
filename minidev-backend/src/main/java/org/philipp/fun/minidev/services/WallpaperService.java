package org.philipp.fun.minidev.services;

import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.pipelines.wallpaperPipeline.WallPaperPipeline;
import org.philipp.fun.minidev.model.Wallpaper;
import org.philipp.fun.minidev.repository.WallpaperRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional()
    public Optional<Wallpaper> getRandomWallpaper() {
        Optional<Wallpaper> wallpaper = wallpaperRepository.findRandomWallpaper();
        if (wallpaper.isEmpty()) {
            generateNewWallpaper();
            return wallpaperRepository.findRandomWallpaper();
        }
        return wallpaper;
    }

    @Transactional(readOnly = true)
    public java.util.List<Wallpaper> getAllWallpapers() {
        return wallpaperRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Wallpaper> getWallpaperById(Long id) {
        return wallpaperRepository.findById(id);
    }

    @Scheduled(cron = "${minidev.wallpaper.cron:0 0 0 * * *}")
    @Transactional
    public void generateDailyWallpaper() {
        log.info("Generating daily wallpaper.");
        generateNewWallpaper();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateNewWallpaper() {
        PipelineContext context = new PipelineContext();
        try {
            boolean success = wallPaperPipeline.execute(context);
            if (!success) {
                log.error("Failed to generate wallpaper: Pipeline execution returned false");
            }
        } catch (Exception e) {
            log.error("Error during wallpaper generation: {}", e.getMessage(), e);
        }
    }
}
