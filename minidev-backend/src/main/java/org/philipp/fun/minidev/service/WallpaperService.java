package org.philipp.fun.minidev.service;

import org.philipp.fun.minidev.model.Wallpaper;
import org.philipp.fun.minidev.repository.WallpaperRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class WallpaperService {

    private final WallpaperRepository wallpaperRepository;

    public WallpaperService(WallpaperRepository wallpaperRepository) {
        this.wallpaperRepository = wallpaperRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Wallpaper> getRandomWallpaper() {
        return wallpaperRepository.findRandomWallpaper();
    }

    @Transactional(readOnly = true)
    public Optional<Wallpaper> getLatestWallpaper() {
        return wallpaperRepository.findTopByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Wallpaper> getAllWallpapers() {
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
}
