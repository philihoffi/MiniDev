package org.philipp.fun.minidev.user.dto;

import org.philipp.fun.minidev.user.model.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing user.
 *
 * @param username    the new username (must not be blank)
 * @param password    the new password (optional; 8-128 characters)
 * @param displayName the new display name (must not be blank)
 * @param role        the new role (must not be null)
 */
public record UpdateUserRequest(

        @NotBlank(message = "Username must not be blank")
        @Size(max = 100, message = "Username must be at most 100 characters")
        String username,

        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        String password,

        @NotBlank(message = "Display name must not be blank")
        @Size(max = 100, message = "Display name must be at most 100 characters")
        String displayName,

        @NotNull(message = "Role must not be null")
        Role role
) {
}