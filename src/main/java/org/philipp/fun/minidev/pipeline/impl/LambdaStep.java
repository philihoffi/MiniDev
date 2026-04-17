package org.philipp.fun.minidev.pipeline.impl;

import org.philipp.fun.minidev.pipeline.abstracts.AbstractStep;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.StepFunction;

public class LambdaStep extends AbstractStep {
    private final StepFunction function;

    public LambdaStep(String name, StepFunction function) {
        super(name);
        this.function = function;
    }

    @Override
    public boolean execute(PipelineContext context) throws Exception {
        if (context == null) return false;
        return function.execute(context);
    }
}
