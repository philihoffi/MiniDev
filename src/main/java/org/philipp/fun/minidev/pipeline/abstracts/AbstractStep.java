package org.philipp.fun.minidev.pipeline.abstracts;

import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.Step;
import org.philipp.fun.minidev.pipeline.model.StepResult;

public abstract class AbstractStep extends AbstractPipelineElement implements Step {

    private StepResult cachedResult;

    protected AbstractStep(String name) {
        super(name);
    }

    @Override
    public final StepResult execute(PipelineContext context) throws Exception {
        if (cachedResult != null) {
            return cachedResult;
        }
        validateContext(context);
        cachedResult = doExecute(context);
        return cachedResult;
    }

    @Override
    public StepResult getCachedResult() {
        return cachedResult;
    }

    protected void validateContext(PipelineContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
    }

    protected abstract StepResult doExecute(PipelineContext context) throws Exception;
}
