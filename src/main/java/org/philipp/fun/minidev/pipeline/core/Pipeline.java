package org.philipp.fun.minidev.pipeline.core;

import org.philipp.fun.minidev.pipeline.model.PipelineResult;
import org.philipp.fun.minidev.pipeline.impl.DefaultPipeline;

import java.util.List;

public interface Pipeline extends PipelineElement {
    static Pipeline create(String name) {
        return new DefaultPipeline(name);
    }

    List<Stage> getStages();
    Pipeline addStage(Stage stage);
    Pipeline addListener(PipelineListener listener);
    default Pipeline addStage(String name, java.util.function.Consumer<Stage> stageBuilder) {
        Stage stage = new org.philipp.fun.minidev.pipeline.impl.DefaultStage(name);
        stageBuilder.accept(stage);
        return addStage(stage);
    }
    PipelineResult execute(PipelineContext context);
}
