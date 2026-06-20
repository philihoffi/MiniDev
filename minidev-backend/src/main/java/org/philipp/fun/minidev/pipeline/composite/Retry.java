package org.philipp.fun.minidev.pipeline.composite;

import org.philipp.fun.minidev.pipeline.core.PipelineContext;

/**
 * A composite element that retries its child sequence up to a configurable
 * number of times on failure.
 */
public class Retry extends Sequence {

    /** Maximum number of retry attempts (excluding the first execution). */
    private final int maxRetries;

    /**
     * Constructs a {@code Retry} element.
     *
     * @param name       the name of this element
     * @param maxRetries the maximum number of retry attempts
     */
    public Retry(String name, int maxRetries) {
        super(name);
        this.maxRetries = maxRetries;
    }

    /**
     * Executes the child elements, retrying up to {@code maxRetries} times if
     * execution fails.
     *
     * @param ctx the pipeline context
     * @return {@code true} if execution succeeded, {@code false} if all
     *         attempts failed
     */
    @Override
    public boolean execute(PipelineContext ctx) {
        for (int i = 0; i <= maxRetries; i++) {
            if (super.execute(ctx)) {
                return true;
            }
            if (i < maxRetries) {
                notifyWarning(this, ctx,
                        "Retry attempt " + (i + 1) + " for " + getName());
            }
        }
        return false;
    }
}