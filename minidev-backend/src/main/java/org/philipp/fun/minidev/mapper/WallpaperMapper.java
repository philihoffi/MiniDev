package org.philipp.fun.minidev.mapper;

import org.philipp.fun.minidev.dto.WallpaperResponse;
import org.philipp.fun.minidev.model.Wallpaper;

/**
 * Utility mapper that converts between {@link Wallpaper} entities and DTOs.
 */
public final class WallpaperMapper {

    /** Private constructor to prevent instantiation. */
    private WallpaperMapper() {
    }

    /**
     * Converts a {@link Wallpaper} entity into a {@link WallpaperResponse} DTO.
     *
     * @param wallpaper the wallpaper entity
     * @return the response DTO
     */
    public static WallpaperResponse toResponse(Wallpaper wallpaper) {
        return new WallpaperResponse(
                wallpaper.getId(),
                wallpaper.getTheme(),
                wallpaper.getCode());
    }
}