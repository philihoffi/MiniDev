package org.philipp.fun.minidev.pipeline.core;

/**
 * Listener interface for pipeline element lifecycle events.
 */
public interface PipelineListener {

    /**
     * Called when a pipeline element starts execution.
     *
     * @param element the element that started
     * @param context the pipeline context
     */
    default void onStart(PipelineElement element, PipelineContext context) {}

    /**
     * Called when a pipeline element finishes execution.
     *
     * @param element the element that finished
     * @param context the pipeline context
     * @param success whether execution was successful
     */
    default void onEnd(PipelineElement element, PipelineContext context, boolean success) {}

    /**
     * Called when a pipeline element issues a warning.
     *
     * @param element the element that issued the warning
     * @param context the pipeline context
     * @param message the warning message
     */
    default void onWarning(PipelineElement element, PipelineContext context, String message) {}

    /**
     * Called when a pipeline element encounters an error.
     *
     * @param element the element that errored
     * @param context the pipeline context
     * @param e       the exception that occurred
     */
    default void onError(PipelineElement element, PipelineContext context, Exception e) {}
}
