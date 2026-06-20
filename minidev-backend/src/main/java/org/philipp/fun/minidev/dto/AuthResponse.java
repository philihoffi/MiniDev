package org.philipp.fun.minidev.dto;

/**
 * Authentication response DTO returned after a successful login.
 *
 * @param id           the user's unique identifier
 * @param username     the username
 * @param displayName  the user's display name
 * @param role         the assigned role
 */
public record AuthResponse(String id, String username, String displayName, String role) {
}