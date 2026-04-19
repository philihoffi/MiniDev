package org.philipp.fun.minidev.pipeline.core;

public interface PipelineListener {
    default void onStepStart(PipelineElement step, PipelineContext context) {}
    default void onStepEnd(PipelineElement step, PipelineContext context, boolean result) {}

    default void onWarning(PipelineElement element, PipelineContext context, String message) {}
    default void onError(PipelineElement element, PipelineContext context, Exception e) {}
}
