package org.philipp.fun.minidev.config;

import org.philipp.fun.minidev.page.model.Page;
import org.philipp.fun.minidev.page.repository.PageRepository;
import org.philipp.fun.minidev.user.model.Role;
import org.philipp.fun.minidev.user.model.User;
import org.philipp.fun.minidev.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds the database with default users when the {@code dev} profile is active.
 */
@Configuration
@Profile("dev")
public class DataInitializer {

    /**
     * Creates a {@link CommandLineRunner} bean that inserts default admin and
     * user accounts if none exist.
     *
     * @param userRepository   the user repository
     * @param pageRepository   the page repository
     * @param passwordEncoder  the password encoder
     * @return a command-line runner that initialises data
     */
    @Bean
    public CommandLineRunner initData(
            UserRepository userRepository,
            PageRepository pageRepository,
            PasswordEncoder passwordEncoder) {
        return ignoredArgs -> {
            if (userRepository.count() == 0) {
                userRepository.save(new User("admin",
                        passwordEncoder.encode("password"), "Admin", Role.ADMIN));
                userRepository.save(new User("user",
                        passwordEncoder.encode("password"), "User", Role.USER));
            }
            if (pageRepository.count() == 0) {
                pageRepository.save(new Page("/dashboard", "Dashboard", "dashboard", "DashboardComponent", null, 10));
                pageRepository.save(new Page("/wallpaper-gallery", "Wallpaper Gallery", "wallpaper", "WallpaperGalleryComponent", null, 20));
                pageRepository.save(new Page("/user-management", "User Management", "people", "UserManagementComponent", "ADMIN", 30));
            }
        };
    }
}