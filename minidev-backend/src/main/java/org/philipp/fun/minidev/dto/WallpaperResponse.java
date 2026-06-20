package org.philipp.fun.minidev.dto;

/**
 * Response DTO containing wallpaper data.
 *
 * @param id    the wallpaper identifier
 * @param theme the wallpaper theme
 * @param code  the generated wallpaper code
 */
public record WallpaperResponse(long id, String theme, String code) {
}