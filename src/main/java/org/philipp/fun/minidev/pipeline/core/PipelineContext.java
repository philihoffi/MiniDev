package org.philipp.fun.minidev.pipeline.core;

import java.util.HashMap;
import java.util.Map;

public class PipelineContext {
    private final Map<String, Object> values = new HashMap<>();
    private Pipeline pipeline;

    public void setPipeline(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    public Pipeline getPipeline() {
        return pipeline;
    }

    public <T> void put(ContextKey<T> key, T value) {
        values.put(key.name(), value);
    }

    public <T> T get(ContextKey<T> key) {
        return get(key.name(), key.type());
    }

    public boolean contains(ContextKey<?> key) {
        return contains(key.name());
    }

    public void put(String key, Object value) {
        values.put(key, value);
    }

    public Object get(String key) {
        return values.get(key);
    }

    public <T> T get(String key, Class<T> type) {
        Object value = values.get(key);
        if (value == null) {
            return null;
        }
        return type.cast(value);
    }

    public boolean contains(String key) {
        return values.containsKey(key);
    }
}
