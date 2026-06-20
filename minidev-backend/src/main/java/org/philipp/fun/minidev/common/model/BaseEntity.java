package org.philipp.fun.minidev.common.model;

import java.time.LocalDateTime;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * Base entity with common fields for all persistent entities.
 */
@MappedSuperclass
public abstract class BaseEntity {

    /** Unique identifier. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    /** Timestamp when the entity was created. */
    private LocalDateTime createdAt;

    /** Timestamp when the entity was last updated. */
    private LocalDateTime updatedAt;

    /**
     * Sets timestamps before persist.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Updates the updatedAt timestamp before update.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Returns the entity ID.
     *
     * @return the ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the creation timestamp.
     *
     * @return the creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp.
     *
     * @param createdAt the creation timestamp
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Returns the last-updated timestamp.
     *
     * @return the last-updated timestamp
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the last-updated timestamp.
     *
     * @param updatedAt the last-updated timestamp
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}