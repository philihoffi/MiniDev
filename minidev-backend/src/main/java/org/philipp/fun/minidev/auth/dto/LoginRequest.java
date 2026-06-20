package org.philipp.fun.minidev.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login request DTO.
 *
 * @param username the username (must not be blank)
 * @param password the password (must not be blank)
 */
public record LoginRequest(

        @NotBlank(message = "Username must not be blank")
        @Size(max = 100, message = "Username must be at most 100 characters")
        String username,

        @NotBlank(message = "Password must not be blank")
        @Size(max = 128, message = "Password must be at most 128 characters")
        String password
) {
}