package org.philipp.fun.minidev.dto;

import org.philipp.fun.minidev.model.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new user.
 *
 * @param username    the desired username (must not be blank)
 * @param password    the desired password (must not be blank; 8-128 characters)
 * @param displayName the display name (must not be blank)
 * @param role        the role to assign (must not be null)
 */
public record CreateUserRequest(

        @NotBlank(message = "Username must not be blank")
        @Size(max = 100, message = "Username must be at most 100 characters")
        String username,

        @NotBlank(message = "Password must not be blank")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        String password,

        @NotBlank(message = "Display name must not be blank")
        @Size(max = 100, message = "Display name must be at most 100 characters")
        String displayName,

        @NotNull(message = "Role must not be null")
        Role role
) {
}