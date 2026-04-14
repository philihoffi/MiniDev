package org.philipp.fun.minidev.pipeline.core;

import java.util.Objects;

public record ContextKey<T>(String name, Class<T> type) {
    public ContextKey(String name, Class<T> type) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
    }

    @Override
    public String toString() {
        return "ContextKey{name='" + name + "', type=" + type.getName() + "}";
    }
}
