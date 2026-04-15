package org.philipp.fun.minidev.pipeline.core;

import org.philipp.fun.minidev.pipeline.model.PipelineResult;
import org.philipp.fun.minidev.pipeline.impl.DefaultPipeline;

import java.util.List;
import java.util.function.Consumer;

public interface Pipeline extends Stage {
    static Pipeline create(String name) {
        return new DefaultPipeline(name);
    }

    Pipeline addListener(PipelineListener listener);

    @Override
    Pipeline addStage(String name, Consumer<Stage> stageBuilder);

    @Override
    Pipeline addStep(String name, StepFunction function);

    @Override
    Pipeline addElement(PipelineElement element);

    default PipelineResult execute() {
        return execute(new PipelineContext());
    }
}
