package org.philipp.fun.minidev.controller;

import java.util.List;

import org.philipp.fun.minidev.dto.AuthResponse;
import org.philipp.fun.minidev.dto.CreateUserRequest;
import org.philipp.fun.minidev.dto.UpdateUserRequest;
import org.philipp.fun.minidev.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * REST controller for admin user management.
 */
@RestController
@RequestMapping("/api/admin")
public class UserController {

    /** User service. */
    private final UserService userService;

    /**
     * Constructs a UserController.
     *
     * @param userService the user service
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Returns all users.
     *
     * @return list of all users
     */
    @GetMapping("/users")
    public List<AuthResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    /**
     * Creates a new user.
     *
     * @param request the create user request
     * @return the created user
     */
    @PostMapping("/users")
    public ResponseEntity<AuthResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    /**
     * Updates an existing user.
     *
     * @param id      the user ID
     * @param request the update user request
     * @return the updated user
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<AuthResponse> updateUser(@PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    /**
     * Deletes a user.
     *
     * @param id the user ID
     * @return no content response
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}