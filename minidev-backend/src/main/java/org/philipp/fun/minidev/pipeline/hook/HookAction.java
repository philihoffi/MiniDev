package org.philipp.fun.minidev.pipeline.hook;

import java.util.function.Supplier;

import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;

/**
 * Hook action interface for intercepting pipeline element execution.
 */
public interface HookAction {

    /**
     * Called before the element executes.
     *
     * @param element the element about to execute
     * @param context the pipeline context
     */
    default void before(PipelineElement element, PipelineContext context) {}

    /**
     * Called after the element executes.
     *
     * @param element the element that executed
     * @param context the pipeline context
     * @param success whether execution succeeded
     */
    default void after(PipelineElement element, PipelineContext context, boolean success) {}

    /**
     * Wraps the element execution. The default implementation simply delegates.
     *
     * @param element   the element to execute
     * @param context   the pipeline context
     * @param execution the original execution supplier
     * @return the result of the execution
     * @throws Exception if execution fails
     */
    default boolean around(PipelineElement element, PipelineContext context, Supplier<Boolean> execution)
            throws Exception {
        return execution.get();
    }
}
