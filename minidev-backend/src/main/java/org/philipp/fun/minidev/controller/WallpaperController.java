package org.philipp.fun.minidev.controller;

import static org.philipp.fun.minidev.mapper.WallpaperMapper.toResponse;

import java.util.List;
import java.util.stream.Collectors;

import org.philipp.fun.minidev.dto.WallpaperResponse;
import org.philipp.fun.minidev.mapper.WallpaperMapper;
import org.philipp.fun.minidev.service.WallpaperGenerationService;
import org.philipp.fun.minidev.service.WallpaperService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for wallpaper-related endpoints.
 */
@RestController
@RequestMapping("/api/wallpaper")
public class WallpaperController {

    /** Service for wallpaper CRUD operations. */
    private final WallpaperService wallpaperService;

    /** Service for wallpaper generation. */
    private final WallpaperGenerationService wallpaperGenerationService;

    /**
     * Constructs a new WallpaperController.
     *
     * @param wallpaperService           the wallpaper service
     * @param wallpaperGenerationService the wallpaper generation service
     */
    public WallpaperController(
            WallpaperService wallpaperService,
            WallpaperGenerationService wallpaperGenerationService) {
        this.wallpaperService = wallpaperService;
        this.wallpaperGenerationService = wallpaperGenerationService;
    }

    /**
     * Returns a random wallpaper.
     *
     * @return the response entity
     */
    @GetMapping("/random")
    public ResponseEntity<WallpaperResponse> getRandomWallpaper() {
        return wallpaperService.getRandomWallpaper()
                .map(wallpaper -> ResponseEntity.ok(toResponse(wallpaper)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Generates and returns the latest wallpaper.
     *
     * @return the response entity
     */
    @GetMapping("/new")
    public ResponseEntity<WallpaperResponse> getNewWallpaper() {
        wallpaperGenerationService.generateNewWallpaper();
        return wallpaperService.getLatestWallpaper()
                .map(wallpaper -> ResponseEntity.ok(toResponse(wallpaper)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Returns all wallpapers.
     *
     * @return the list of wallpaper responses
     */
    @GetMapping
    public ResponseEntity<List<WallpaperResponse>> getAllWallpapers() {
        List<WallpaperResponse> wallpapers =
                wallpaperService.getAllWallpapers().stream()
                .map(WallpaperMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(wallpapers);
    }

    /**
     * Returns a wallpaper by its ID.
     *
     * @param id the wallpaper ID
     * @return the response entity
     */
    @GetMapping("/{id}")
    public ResponseEntity<WallpaperResponse> getWallpaperById(
            @PathVariable Long id) {
        return wallpaperService.getWallpaperById(id)
                .map(w -> ResponseEntity.ok(toResponse(w)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Deletes a wallpaper by its ID.
     *
     * @param id the wallpaper ID
     * @return the response entity
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWallpaper(@PathVariable Long id) {
        wallpaperService.deleteWallpaper(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Generates a specified number of wallpapers.
     *
     * @param count the number of wallpapers to generate
     * @return the response entity
     */
    @PostMapping("/generate")
    public ResponseEntity<Void> generateWallpaper(
            @RequestParam(defaultValue = "1") int count) {
        for (int i = 0; i < count; i++) {
            wallpaperGenerationService.enqueueWallpaperGeneration();
        }
        return ResponseEntity.accepted().build();
    }
}