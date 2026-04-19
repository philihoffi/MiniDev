package org.philipp.fun.minidev.web.objects;

import org.philipp.fun.minidev.spring.model.Role;

public record UserRequest(String username, String password, String displayName, Role role) {}
