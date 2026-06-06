package org.philipp.fun.minidev.pipeline.core;

import java.util.Objects;

public record ContextKey<T>(String name, Class<T> type) {
    public ContextKey {
        Objects.requireNonNull(name);
        Objects.requireNonNull(type);
    }
}
