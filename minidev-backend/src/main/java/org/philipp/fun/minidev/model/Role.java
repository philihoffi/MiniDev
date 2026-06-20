package org.philipp.fun.minidev.model;

/**
 * Enumeration of user roles within the application.
 */
public enum Role {

    /** Administrator with full system access. */
    ADMIN,
    /** Standard application user. */
    USER,
    /** Unauthenticated or limited-access user. */
    GUEST
}