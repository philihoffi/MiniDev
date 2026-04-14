package org.philipp.fun.minidev.pipeline.core;

import org.philipp.fun.minidev.pipeline.model.StepResult;

@FunctionalInterface
public interface StepFunction {
    StepResult execute(PipelineContext context) throws Exception;
}
