package org.philipp.fun.minidev.pipeline.core;

import java.util.HashMap;

/**
 * Execution context for a pipeline, backed by a {@link HashMap}.
 */
public class PipelineContext extends HashMap<ContextKey<?>, Object> {

    /** The root pipeline element being executed. */
    private PipelineElement pipeline;

    /**
     * Sets the pipeline element associated with this context.
     *
     * @param pipeline the pipeline element
     */
    public void setPipeline(PipelineElement pipeline) {
        this.pipeline = pipeline;
    }

    /**
     * Returns the pipeline element associated with this context.
     *
     * @return the pipeline element
     */
    public PipelineElement getPipeline() {
        return pipeline;
    }

    /**
     * Stores a typed value in the context.
     *
     * @param <T>   the value type
     * @param key   the context key
     * @param value the value to store
     */
    public <T> void putValue(ContextKey<T> key, T value) {
        super.put(key, value);
    }

    /**
     * Retrieves a typed value from the context.
     *
     * @param <T> the value type
     * @param key the context key
     * @return the value, or {@code null} if not present
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue(ContextKey<T> key) {
        Object val = super.get(key);
        return val != null ? (T) key.type().cast(val) : null;
    }
}
