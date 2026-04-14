package org.philipp.fun.minidev.pipeline.abstracts;

import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.Step;
import org.philipp.fun.minidev.pipeline.model.StepResult;

public abstract class AbstractStep extends AbstractPipelineElement implements Step {

    protected AbstractStep(String name) {
        super(name);
    }

    @Override
    public final StepResult execute(PipelineContext context) throws Exception {
        validateContext(context);
        return doExecute(context);
    }

    protected void validateContext(PipelineContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
    }

    protected abstract StepResult doExecute(PipelineContext context) throws Exception;
}
