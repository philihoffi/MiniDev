package org.philipp.fun.minidev.pipeline.core;

import org.philipp.fun.minidev.pipeline.model.PipelineResult;
import org.philipp.fun.minidev.pipeline.model.StageResult;
import org.philipp.fun.minidev.pipeline.model.StepResult;

public interface PipelineListener {
    default void onPipelineStart(Pipeline pipeline, PipelineContext context) {}
    default void onPipelineEnd(Pipeline pipeline, PipelineContext context, PipelineResult result) {}

    default void onStageStart(Stage stage, PipelineContext context) {}
    default void onStageEnd(Stage stage, PipelineContext context, StageResult result) {}

    default void onStepStart(Step step, PipelineContext context) {}
    default void onStepEnd(Step step, PipelineContext context, StepResult result) {}
    
    default void onError(PipelineElement element, PipelineContext context, Exception e) {}
}
