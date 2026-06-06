package org.philipp.fun.minidev.pipeline.hook;

import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;

import java.util.function.Supplier;

public interface HookAction {
    default void before(PipelineElement element, PipelineContext context) {}
    default void after(PipelineElement element, PipelineContext context, boolean success) {}
    default boolean around(PipelineElement element, PipelineContext context, Supplier<Boolean> execution) throws Exception {
        return execution.get();
    }
}