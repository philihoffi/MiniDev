package org.philipp.fun.minidev.pipeline.core;

public interface PipelineListener {
    default void onStart(PipelineElement element, PipelineContext context) {}
    default void onEnd(PipelineElement element, PipelineContext context, boolean success) {}
    default void onWarning(PipelineElement element, PipelineContext context, String message) {}
    default void onError(PipelineElement element, PipelineContext context, Exception e) {}
}
