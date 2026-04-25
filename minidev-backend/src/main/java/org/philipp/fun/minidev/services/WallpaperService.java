package org.philipp.fun.minidev.services;

import org.philipp.fun.minidev.model.Wallpaper;
import org.philipp.fun.minidev.repository.WallpaperRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class WallpaperService {

    private static final Logger log = LoggerFactory.getLogger(WallpaperService.class);

    private final WallpaperRepository wallpaperRepository;
    private final WallpaperGenerationService wallpaperGenerationService;
    private final PipelineExecutionQueueService pipelineExecutionQueueService;

    public WallpaperService(
            WallpaperRepository wallpaperRepository,
            WallpaperGenerationService wallpaperGenerationService,
            PipelineExecutionQueueService pipelineExecutionQueueService
    ) {
        this.wallpaperRepository = wallpaperRepository;
        this.wallpaperGenerationService = wallpaperGenerationService;
        this.pipelineExecutionQueueService = pipelineExecutionQueueService;
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
    public Optional<Wallpaper> getLatestWallpaper() {
        return wallpaperRepository.findTopByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public java.util.List<Wallpaper> getAllWallpapers() {
        return wallpaperRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Wallpaper> getWallpaperById(Long id) {
        return wallpaperRepository.findById(id);
    }

    @Transactional
    public void deleteWallpaper(Long id) {
        wallpaperRepository.deleteById(id);
    }

    @Scheduled(cron = "${minidev.wallpaper.cron:0 0 0 * * *}")
    @Transactional
    public void generateDailyWallpaper() {
        log.info("Generating daily wallpaper.");
        enqueueWallpaperGeneration();
    }

    public void generateNewWallpaper() {
        CompletableFuture<Boolean> generationFuture = enqueueWallpaperGeneration();
        try {
            boolean success = generationFuture.get();
            if (!success) {
                log.warn("Wallpaper generation finished without success");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Wallpaper generation was interrupted while waiting for queue execution", e);
        } catch (ExecutionException e) {
            log.error("Wallpaper generation failed in queue execution", e.getCause());
        }
    }

    public CompletableFuture<Boolean> enqueueWallpaperGeneration() {
        return pipelineExecutionQueueService.submit(wallpaperGenerationService::generateNewWallpaperInNewTransaction);
    }
}
