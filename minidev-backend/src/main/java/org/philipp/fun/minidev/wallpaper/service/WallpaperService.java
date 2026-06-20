package org.philipp.fun.minidev.wallpaper.service;

import java.util.List;
import java.util.Optional;

import org.philipp.fun.minidev.wallpaper.model.Wallpaper;
import org.philipp.fun.minidev.wallpaper.repository.WallpaperRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for wallpaper operations.
 */
@Service
public class WallpaperService {

    /** Wallpaper repository. */
    private final WallpaperRepository wallpaperRepository;

    /**
     * Constructs a WallpaperService.
     *
     * @param wallpaperRepository the wallpaper repository
     */
    public WallpaperService(WallpaperRepository wallpaperRepository) {
        this.wallpaperRepository = wallpaperRepository;
    }

    /**
     * Returns a random wallpaper.
     *
     * @return an optional containing the wallpaper, or empty if none found
     */
    @Transactional(readOnly = true)
    public Optional<Wallpaper> getRandomWallpaper() {
        return wallpaperRepository.findRandomWallpaper();
    }

    /**
     * Returns the latest wallpaper.
     *
     * @return an optional containing the wallpaper, or empty if none found
     */
    @Transactional(readOnly = true)
    public Optional<Wallpaper> getLatestWallpaper() {
        return wallpaperRepository.findTopByOrderByCreatedAtDesc();
    }

    /**
     * Returns all wallpapers.
     *
     * @return list of all wallpapers
     */
    @Transactional(readOnly = true)
    public List<Wallpaper> getAllWallpapers() {
        return wallpaperRepository.findAll();
    }

    /**
     * Returns a wallpaper by ID.
     *
     * @param id the wallpaper ID
     * @return an optional containing the wallpaper, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<Wallpaper> getWallpaperById(Long id) {
        return wallpaperRepository.findById(id);
    }

    /**
     * Deletes a wallpaper by ID.
     *
     * @param id the wallpaper ID
     */
    @Transactional
    public void deleteWallpaper(Long id) {
        wallpaperRepository.deleteById(id);
    }
}