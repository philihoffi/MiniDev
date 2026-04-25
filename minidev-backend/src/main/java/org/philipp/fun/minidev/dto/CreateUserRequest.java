package org.philipp.fun.minidev.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.philipp.fun.minidev.model.Role;

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

