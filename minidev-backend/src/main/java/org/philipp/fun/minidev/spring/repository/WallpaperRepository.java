package org.philipp.fun.minidev.spring.repository;

import org.philipp.fun.minidev.spring.model.Wallpaper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface WallpaperRepository extends JpaRepository<Wallpaper,Long> {
    Optional<Wallpaper> findTopByOrderByCreatedAtDesc();

    @Query(value = "SELECT * FROM wallpapers ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<Wallpaper> findRandomWallpaper();
}
