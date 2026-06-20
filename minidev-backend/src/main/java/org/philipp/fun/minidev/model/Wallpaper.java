package org.philipp.fun.minidev.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * JPA entity representing a generated wallpaper.
 */
@Entity
@Table(name = "wallpapers")
public class Wallpaper extends BaseEntity {

    /** The theme or topic of the wallpaper. */
    @Column(columnDefinition = "TEXT")
    private String theme;

    /** The generated code (HTML/CSS/JS) for the wallpaper. */
    @Column(columnDefinition = "TEXT")
    private String code;

    /**
     * Returns the wallpaper theme.
     *
     * @return the theme
     */
    public String getTheme() {
        return theme;
    }

    /**
     * Sets the wallpaper theme.
     *
     * @param theme the theme to set
     */
    public void setTheme(String theme) {
        this.theme = theme;
    }

    /**
     * Returns the generated wallpaper code.
     *
     * @return the code
     */
    public String getCode() {
        return code;
    }

    /**
     * Sets the generated wallpaper code.
     *
     * @param code the code to set
     */
    public void setCode(String code) {
        this.code = code;
    }
}