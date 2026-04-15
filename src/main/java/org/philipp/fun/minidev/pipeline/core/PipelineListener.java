package org.philipp.fun.minidev.pipeline.core;

import org.philipp.fun.minidev.pipeline.model.PipelineResult;

public interface PipelineListener {
    default void onPipelineStart(Pipeline pipeline, PipelineContext context) {}
    default void onPipelineEnd(Pipeline pipeline, PipelineContext context, PipelineResult result) {}

    default void onStepStart(PipelineElement step, PipelineContext context) {}
    default void onStepEnd(PipelineElement step, PipelineContext context, PipelineResult result) {}

    default void onWarning(PipelineElement element, PipelineContext context, String message) {}
    default void onError(PipelineElement element, PipelineContext context, Exception e) {}
}
