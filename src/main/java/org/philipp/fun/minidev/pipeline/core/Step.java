package org.philipp.fun.minidev.pipeline.core;

import org.philipp.fun.minidev.pipeline.impl.LambdaStep;
import org.philipp.fun.minidev.pipeline.model.PipelineResult;

public interface Step extends PipelineElement {
    static Step create(String name, StepFunction function) {
        return new LambdaStep(name, function);
    }
}
