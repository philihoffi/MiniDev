package org.philipp.fun.minidev.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreativeBrief(
        String theme,
        String description,
        @JsonProperty("color_palette") List<String> colorPalette,
        String mood,
        @JsonProperty("motion_style") String motionStyle,
        @JsonProperty("key_elements") List<String> keyElements
) {
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
