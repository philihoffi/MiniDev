package org.philipp.fun.minidev.web.controller;

import org.philipp.fun.minidev.spring.model.User;
import org.philipp.fun.minidev.spring.repository.UserRepository;
import org.philipp.fun.minidev.web.objects.AuthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    public List<AuthResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> new AuthResponse(
                        String.valueOf(user.getId()),
                        user.getDisplayName(),
                        user.getRole().name()
                ))
                .collect(Collectors.toList());
    }
}
