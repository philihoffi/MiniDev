package org.philipp.fun.minidev.auth.controller;

import org.philipp.fun.minidev.auth.dto.AuthResponse;
import org.philipp.fun.minidev.auth.dto.LoginRequest;
import org.philipp.fun.minidev.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * REST controller for authentication endpoints.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /** The authentication service. */
    private final AuthService authService;

    /**
     * Constructs an AuthController.
     *
     * @param authService the authentication service
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Handles user login.
     *
     * @param request         the login request body
     * @param servletRequest  the HTTP servlet request
     * @param servletResponse the HTTP servlet response
     * @return the authentication response
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        return ResponseEntity.ok(authService.login(request, servletRequest, servletResponse));
    }

    /**
     * Handles user logout.
     *
     * @param request the HTTP servlet request
     * @return an empty OK response
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authService.logout(request);
        return ResponseEntity.ok().build();
    }
}
