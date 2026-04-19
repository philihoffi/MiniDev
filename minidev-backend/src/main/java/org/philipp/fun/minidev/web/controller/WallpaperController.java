package org.philipp.fun.minidev.web.controller;

import org.philipp.fun.minidev.services.WallpaperService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/wallpaper")
public class WallpaperController {

    private final WallpaperService wallpaperService;

    public WallpaperController(WallpaperService wallpaperService) {
        this.wallpaperService = wallpaperService;
    }

    @GetMapping("/latest")
    public ResponseEntity<?> getLatestWallpaper() {
        return wallpaperService.getRandomWallpaper()
                .map(wallpaper -> ResponseEntity.ok(Map.of("code", wallpaper.getCode())))
                .orElse(ResponseEntity.notFound().build());
    }
}
