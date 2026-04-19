package org.philipp.fun.minidev.spring;

import org.philipp.fun.minidev.spring.model.Role;
import org.philipp.fun.minidev.spring.model.User;
import org.philipp.fun.minidev.spring.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                userRepository.save(new User("admin", passwordEncoder.encode("password"), "Admin", Role.ADMIN));
                userRepository.save(new User("user", passwordEncoder.encode("password"), "User", Role.USER));
            }
        };
    }
}
