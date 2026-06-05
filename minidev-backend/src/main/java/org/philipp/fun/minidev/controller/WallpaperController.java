package org.philipp.fun.minidev.controller;

import org.philipp.fun.minidev.dto.WallpaperResponse;
import org.philipp.fun.minidev.service.WallpaperGenerationService;
import org.philipp.fun.minidev.mapper.WallpaperMapper;
import org.philipp.fun.minidev.service.WallpaperService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.philipp.fun.minidev.mapper.WallpaperMapper.toResponse;

@RestController
@RequestMapping("/api/wallpaper")
public class WallpaperController {

    private final WallpaperService wallpaperService;
    private final WallpaperGenerationService wallpaperGenerationService;

    public WallpaperController(WallpaperService wallpaperService, WallpaperGenerationService wallpaperGenerationService) {
        this.wallpaperService = wallpaperService;
        this.wallpaperGenerationService = wallpaperGenerationService;
    }

    @GetMapping("/random")
    public ResponseEntity<WallpaperResponse> getRandomWallpaper() {
        return wallpaperService.getRandomWallpaper()
                .map(wallpaper -> ResponseEntity.ok(toResponse(wallpaper)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/new")
    public ResponseEntity<WallpaperResponse> getNewWallpaper() {
        wallpaperGenerationService.generateNewWallpaper();
        return wallpaperService.getLatestWallpaper()
                .map(wallpaper -> ResponseEntity.ok(toResponse(wallpaper)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<WallpaperResponse>> getAllWallpapers() {
        List<WallpaperResponse> wallpapers = wallpaperService.getAllWallpapers().stream()
                .map(WallpaperMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(wallpapers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WallpaperResponse> getWallpaperById(@PathVariable Long id) {
        return wallpaperService.getWallpaperById(id)
                .map(w -> ResponseEntity.ok(toResponse(w)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWallpaper(@PathVariable Long id) {
        wallpaperService.deleteWallpaper(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/generate")
    public ResponseEntity<Void> generateWallpaper(@RequestParam(defaultValue = "1") int count) {
        for (int i = 0; i < count; i++) {
            wallpaperGenerationService.enqueueWallpaperGeneration();
        }
        return ResponseEntity.accepted().build();
    }
}
