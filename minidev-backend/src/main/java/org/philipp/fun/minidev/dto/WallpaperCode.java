package org.philipp.fun.minidev.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WallpaperCode(String html, String css, String js, String description) {
}
