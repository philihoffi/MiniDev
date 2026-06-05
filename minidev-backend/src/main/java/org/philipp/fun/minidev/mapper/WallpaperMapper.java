package org.philipp.fun.minidev.mapper;

import org.philipp.fun.minidev.dto.WallpaperResponse;
import org.philipp.fun.minidev.model.Wallpaper;

public final class WallpaperMapper {

    private WallpaperMapper() {}

    public static WallpaperResponse toResponse(Wallpaper wallpaper) {
        return new WallpaperResponse(wallpaper.getId(), wallpaper.getTheme(), wallpaper.getCode());
    }
}
