package org.philipp.fun.minidev.repository;

import java.util.Optional;

import org.philipp.fun.minidev.model.Wallpaper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Spring Data JPA repository for {@link Wallpaper} entities.
 */
public interface WallpaperRepository extends JpaRepository<Wallpaper, Long> {

    /**
     * Finds the most recently created wallpaper.
     *
     * @return an {@link Optional} containing the latest wallpaper, or empty if none exist
     */
    Optional<Wallpaper> findTopByOrderByCreatedAtDesc();

    /**
     * Returns a random wallpaper using a native random-ordering query.
     *
     * @return an {@link Optional} containing a random wallpaper, or empty if none exist
     */
    @Query(value = "SELECT * FROM wallpapers ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<Wallpaper> findRandomWallpaper();
}