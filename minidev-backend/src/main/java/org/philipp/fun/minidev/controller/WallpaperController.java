package org.philipp.fun.minidev.controller;

import org.philipp.fun.minidev.dto.WallpaperResponse;
import org.philipp.fun.minidev.services.WallpaperService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wallpaper")
public class WallpaperController {

    private final WallpaperService wallpaperService;

    public WallpaperController(WallpaperService wallpaperService) {
        this.wallpaperService = wallpaperService;
    }

    @GetMapping("/latest")
    public ResponseEntity<WallpaperResponse> getLatestWallpaper() {
        return wallpaperService.getRandomWallpaper()
                .map(wallpaper -> ResponseEntity.ok(new WallpaperResponse(wallpaper.getId(), wallpaper.getTheme(), wallpaper.getCode())))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<WallpaperResponse>> getAllWallpapers() {
        List<WallpaperResponse> wallpapers = wallpaperService.getAllWallpapers().stream()
                .map(w -> new WallpaperResponse(w.getId(), w.getTheme(), w.getCode()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(wallpapers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WallpaperResponse> getWallpaperById(@PathVariable Long id) {
        return wallpaperService.getWallpaperById(id)
                .map(w -> ResponseEntity.ok(new WallpaperResponse(w.getId(), w.getTheme(), w.getCode())))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/generate")
    public ResponseEntity<Void> generateWallpaper(@RequestParam(defaultValue = "1") int count) {
        for (int i = 0; i < count; i++){
            wallpaperService.generateNewWallpaper();
        }
        return ResponseEntity.accepted().build();
    }
}
