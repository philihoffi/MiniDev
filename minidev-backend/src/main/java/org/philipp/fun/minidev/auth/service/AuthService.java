package org.philipp.fun.minidev.auth.service;

import java.util.Collections;

import org.philipp.fun.minidev.auth.dto.AuthResponse;
import org.philipp.fun.minidev.auth.dto.LoginRequest;
import org.philipp.fun.minidev.user.model.User;
import org.philipp.fun.minidev.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Service for authentication operations.
 */
@Service
public class AuthService {

    /** User repository. */
    private final UserRepository userRepository;

    /** Password encoder. */
    private final PasswordEncoder passwordEncoder;

    /** Security context repository. */
    private final SecurityContextRepository securityContextRepository;

    /** Security context holder strategy. */
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    /**
     * Constructs an AuthService.
     *
     * @param userRepository            the user repository
     * @param passwordEncoder           the password encoder
     * @param securityContextRepository the security context repository
     */
    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            SecurityContextRepository securityContextRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityContextRepository = securityContextRepository;
    }

    /**
     * Authenticates a user and creates a security session.
     *
     * @param request        the login request
     * @param servletRequest the HTTP servlet request
     * @param servletResponse the HTTP servlet response
     * @return the auth response
     */
    public AuthResponse login(LoginRequest request, HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        User user = userRepository.findByUsername(request.username())
                .filter(foundUser -> passwordEncoder.matches(
                        request.password(), foundUser.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );

        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(auth);
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);

        return new AuthResponse(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole().name()
        );
    }

    /**
     * Logs out the current user by clearing the security context and invalidating the session.
     *
     * @param request the HTTP servlet request
     */
    public void logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}