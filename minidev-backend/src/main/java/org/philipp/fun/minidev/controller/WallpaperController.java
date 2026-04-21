package org.philipp.fun.minidev.controller;

import org.philipp.fun.minidev.dto.WallpaperResponse;
import org.philipp.fun.minidev.services.WallpaperService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
                .map(wallpaper -> ResponseEntity.ok(new WallpaperResponse(wallpaper.getCode())))
                .orElse(ResponseEntity.notFound().build());
    }
}
