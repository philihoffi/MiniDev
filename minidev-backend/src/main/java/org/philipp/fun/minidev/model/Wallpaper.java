package org.philipp.fun.minidev.model;

import jakarta.persistence.*;

@Entity
@Table(name = "wallpapers")
public class Wallpaper extends BaseEntity{

    @Column(columnDefinition = "TEXT")
    private String theme;

    @Column(columnDefinition = "TEXT")
    private String code;

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
