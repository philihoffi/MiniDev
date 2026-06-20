package org.philipp.fun.minidev.user.model;

import org.philipp.fun.minidev.common.model.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Entity representing an application user.
 */
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    /** The unique username. */
    @Column(unique = true, nullable = false)
    private String username;

    /** The hashed password. */
    @Column(nullable = false)
    private String password;

    /** The display name. */
    @Column(nullable = false)
    private String displayName;

    /** The user role. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** Default constructor. */
    public User() {
    }

    /**
     * Constructs a new User.
     *
     * @param username    the username
     * @param password    the password
     * @param displayName the display name
     * @param role        the role
     */
    public User(String username, String password, String displayName, Role role) {
        this.username = username;
        this.password = password;
        this.displayName = displayName;
        this.role = role;
    }

    /**
     * Gets the username.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username.
     *
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the password.
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password.
     *
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Gets the display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the display name.
     *
     * @param displayName the display name to set
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the user role.
     *
     * @return the role
     */
    public Role getRole() {
        return role;
    }

    /**
     * Sets the user role.
     *
     * @param role the role to set
     */
    public void setRole(Role role) {
        this.role = role;
    }
}