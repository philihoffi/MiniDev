package org.philipp.fun.minidev.service;

import java.util.List;

import org.philipp.fun.minidev.dto.AuthResponse;
import org.philipp.fun.minidev.dto.CreateUserRequest;
import org.philipp.fun.minidev.dto.UpdateUserRequest;
import org.philipp.fun.minidev.exception.DuplicateResourceException;
import org.philipp.fun.minidev.exception.ResourceNotFoundException;
import org.philipp.fun.minidev.model.User;
import org.philipp.fun.minidev.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user management operations.
 */
@Service
@Transactional
public class UserService {

    /** User repository. */
    private final UserRepository userRepository;

    /** Password encoder. */
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructs a UserService.
     *
     * @param userRepository   the user repository
     * @param passwordEncoder  the password encoder
     */
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Returns all users.
     *
     * @return list of auth responses
     */
    @Transactional(readOnly = true)
    public List<AuthResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toAuthResponse)
                .toList();
    }

    /**
     * Creates a new user.
     *
     * @param request the create user request
     * @return the created user auth response
     */
    public AuthResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("User", "username", request.username());
        }

        User user = new User(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.displayName(),
                request.role()
        );

        User savedUser = userRepository.save(user);
        return toAuthResponse(savedUser);
    }

    /**
     * Updates an existing user.
     *
     * @param id      the user ID
     * @param request the update user request
     * @return the updated user auth response
     */
    public AuthResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        userRepository.findByUsername(request.username())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(ignoredExisting -> {
                    throw new DuplicateResourceException("User", "username", request.username());
                });

        user.setUsername(request.username());
        user.setDisplayName(request.displayName());
        user.setRole(request.role());

        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        User savedUser = userRepository.save(user);
        return toAuthResponse(savedUser);
    }

    /**
     * Deletes a user by ID.
     *
     * @param id the user ID
     */
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", id);
        }
        userRepository.deleteById(id);
    }

    /**
     * Converts a User entity to an AuthResponse DTO.
     *
     * @param user the user entity
     * @return the auth response
     */
    private AuthResponse toAuthResponse(User user) {
        return new AuthResponse(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole().name()
        );
    }
}