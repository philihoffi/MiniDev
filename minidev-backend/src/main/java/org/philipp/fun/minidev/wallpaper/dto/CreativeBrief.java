package org.philipp.fun.minidev.wallpaper.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data record representing the creative brief for a wallpaper generation
 * request.
 *
 * @param theme        the main theme of the wallpaper
 * @param description  a textual description of the desired output
 * @param colorPalette the list of suggested colours
 * @param mood         the intended mood or atmosphere
 * @param motionStyle  the desired motion style
 * @param keyElements  significant visual elements to include
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CreativeBrief(
        String theme,
        String description,
        @JsonProperty("color_palette") List<String> colorPalette,
        String mood,
        @JsonProperty("motion_style") String motionStyle,
        @JsonProperty("key_elements") List<String> keyElements
) {

    /**
     * Formats the brief as a human-readable multi-line string.
     *
     * @return the formatted brief
     */
    public String format() {
        return String.format("""
                Theme: '%s'
                Description: %s
                Color Palette: %s
                Mood: %s
                Motion Style: %s
                Key Elements: %s""",
                theme, description,
                String.join(", ", colorPalette),
                mood, motionStyle,
                String.join(", ", keyElements));
    }
}