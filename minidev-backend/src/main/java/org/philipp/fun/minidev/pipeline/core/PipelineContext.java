package org.philipp.fun.minidev.pipeline.core;

import java.util.HashMap;
import java.util.Map;

public class PipelineContext extends HashMap<ContextKey<?>, Object> {
    private PipelineElement pipeline;

    public void setPipeline(PipelineElement pipeline) {
        this.pipeline = pipeline;
    }

    public PipelineElement getPipeline() {
        return pipeline;
    }

    public <T> void putValue(ContextKey<T> key, T value) {
        super.put(key, value);
    }

    public <T> T getValue(ContextKey<T> key) {
        Object value = super.get(key);
        if (value == null) {
            return null;
        }
        return key.type().cast(value);
    }

    public boolean containsValue(ContextKey<?> key) {
        return super.containsKey(key);
    }
}
