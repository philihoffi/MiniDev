package org.philipp.fun.minidev.services;

import org.philipp.fun.minidev.model.Wallpaper;
import org.philipp.fun.minidev.repository.WallpaperRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class WallpaperService {

    private static final Logger log = LoggerFactory.getLogger(WallpaperService.class);

    private final WallpaperRepository wallpaperRepository;
    private final WallpaperGenerationService wallpaperGenerationService;

    public WallpaperService(
            WallpaperRepository wallpaperRepository,
            WallpaperGenerationService wallpaperGenerationService
    ) {
        this.wallpaperRepository = wallpaperRepository;
        this.wallpaperGenerationService = wallpaperGenerationService;
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
        generateNewWallpaper();
    }

    public void generateNewWallpaper() {
        boolean success = wallpaperGenerationService.generateNewWallpaperInNewTransaction();
        if (!success) {
            log.warn("Wallpaper generation finished without success");
        }
    }
}
