package org.philipp.fun.minidev.pipeline.composite;

import org.philipp.fun.minidev.pipeline.core.PipelineContext;

public class Retry extends Sequence {
    private final int maxRetries;

    public Retry(String name, int maxRetries) {
        super(name);
        this.maxRetries = maxRetries;
    }

    @Override
    public boolean execute(PipelineContext ctx) {
        for (int i = 0; i <= maxRetries; i++) {
            if (super.execute(ctx)) return true;
            if (i < maxRetries) {
                notifyWarning(this, ctx, "Retry attempt " + (i + 1) + " for " + getName());
            }
        }
        return false;
    }
}
