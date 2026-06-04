package org.philipp.fun.minidev.pipeline;

import java.util.HashMap;

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

    @SuppressWarnings("unchecked")
    public <T> T getValue(ContextKey<T> key) {
        Object val = super.get(key);
        return val != null ? (T) key.type().cast(val) : null;
    }
}
