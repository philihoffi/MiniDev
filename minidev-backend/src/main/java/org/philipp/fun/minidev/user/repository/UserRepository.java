package org.philipp.fun.minidev.user.repository;

import java.util.Optional;

import org.philipp.fun.minidev.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link User} entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their username.
     *
     * @param username the username to search for
     * @return an {@link Optional} containing the found user, or empty if not found
     */
    Optional<User> findByUsername(String username);

    /**
     * Checks whether a user with the given username already exists.
     *
     * @param username the username to check
     * @return {@code true} if a user with the given username exists, {@code false} otherwise
     */
    boolean existsByUsername(String username);
}