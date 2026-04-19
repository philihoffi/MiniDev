package org.philipp.fun.minidev.pipeline.core;

@FunctionalInterface
public interface StepFunction {
    boolean execute(PipelineContext context) throws Exception;
}
