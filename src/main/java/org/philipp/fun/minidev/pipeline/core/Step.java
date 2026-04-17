package org.philipp.fun.minidev.pipeline.core;

import org.philipp.fun.minidev.pipeline.impl.LambdaStep;

public interface Step extends PipelineElement {
    static Step create(String name, StepFunction function) {
        return new LambdaStep(name, function);
    }
}
