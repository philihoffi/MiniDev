package org.philipp.fun.minidev.controller;

import org.philipp.fun.minidev.model.User;
import org.philipp.fun.minidev.repository.UserRepository;
import org.philipp.fun.minidev.dto.AuthResponse;
import org.philipp.fun.minidev.dto.UserRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/users")
    public List<AuthResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @PostMapping("/users")
    public ResponseEntity<AuthResponse> createUser(@RequestBody UserRequest request) {
        User user = new User(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.displayName(),
                request.role()
        );
        User savedUser = userRepository.save(user);
        return ResponseEntity.ok(mapToResponse(savedUser));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<AuthResponse> updateUser(@PathVariable Long id, @RequestBody UserRequest request) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setUsername(request.username());
                    user.setDisplayName(request.displayName());
                    user.setRole(request.role());
                    if (request.password() != null && !request.password().isBlank()) {
                        user.setPassword(passwordEncoder.encode(request.password()));
                    }
                    User savedUser = userRepository.save(user);
                    return ResponseEntity.ok(mapToResponse(savedUser));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    private AuthResponse mapToResponse(User user) {
        return new AuthResponse(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole().name()
        );
    }
}
