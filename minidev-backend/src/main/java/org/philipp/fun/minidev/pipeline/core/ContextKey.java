package org.philipp.fun.minidev.pipeline.core;

import java.util.Objects;

/**
 * A strongly-typed key used to retrieve values from the pipeline context.
 *
 * @param name the key name
 * @param type the value type
 * @param <T>  the type of the value associated with this key
 */
public record ContextKey<T>(String name, Class<T> type) {

    /**
     * Compact constructor that validates the record components.
     */
    public ContextKey {
        Objects.requireNonNull(name);
        Objects.requireNonNull(type);
    }
}