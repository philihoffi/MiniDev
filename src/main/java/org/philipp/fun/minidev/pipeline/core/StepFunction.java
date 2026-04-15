package org.philipp.fun.minidev.pipeline.core;

import org.philipp.fun.minidev.pipeline.model.PipelineResult;

@FunctionalInterface
public interface StepFunction {
    PipelineResult execute(PipelineContext context) throws Exception;
}
