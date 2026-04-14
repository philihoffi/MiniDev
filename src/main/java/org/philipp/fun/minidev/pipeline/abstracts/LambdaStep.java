package org.philipp.fun.minidev.pipeline.abstracts;

import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.StepFunction;
import org.philipp.fun.minidev.pipeline.model.StepResult;

public class LambdaStep extends AbstractStep {
    private final StepFunction function;

    public LambdaStep(String name, StepFunction function) {
        super(name);
        this.function = function;
    }

    @Override
    protected StepResult doExecute(PipelineContext context) throws Exception {
        return function.execute(context);
    }
}
