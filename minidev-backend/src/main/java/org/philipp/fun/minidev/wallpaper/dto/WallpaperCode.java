package org.philipp.fun.minidev.wallpaper.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Data record representing the generated wallpaper code.
 *
 * @param html        the HTML markup
 * @param css         the CSS styles
 * @param js          the JavaScript code
 * @param description a textual description of the wallpaper
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WallpaperCode(String html, String css, String js, String description) {
}