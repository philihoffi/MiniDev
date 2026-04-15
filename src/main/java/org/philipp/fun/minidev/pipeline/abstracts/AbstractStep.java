package org.philipp.fun.minidev.pipeline.abstracts;

import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.Step;
import org.philipp.fun.minidev.pipeline.model.PipelineResult;

public abstract class AbstractStep extends AbstractPipelineElement implements Step {

    protected AbstractStep(String name) {
        super(name);
    }

    @Override
    public PipelineResult execute(PipelineContext context) {
        if (cachedResult != null) {
            return cachedResult;
        }

        if (context == null) {
            return PipelineResult.failed(getName(), "context must not be null", context);
        }

        try {
            cachedResult = doExecute(context);
            return cachedResult;
        } catch (Exception e) {
            notifyError(this, context, e);
            cachedResult = PipelineResult.failed(getName(), e.getMessage(), context);
            return cachedResult;
        }
    }

    protected abstract PipelineResult doExecute(PipelineContext context) throws Exception;

    @Override
    public PipelineResult getCachedResult() {
        return super.getCachedResult();
    }
}
