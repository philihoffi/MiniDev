package org.philipp.fun.minidev.dto;

import org.philipp.fun.minidev.model.Role;

public record UserRequest(String username, String password, String displayName, Role role) {}
