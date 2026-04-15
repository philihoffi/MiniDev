package org.philipp.fun.minidev.pipeline.core;

import org.philipp.fun.minidev.pipeline.impl.SequenzStage;

import java.util.List;
import java.util.function.Consumer;

public interface Stage extends PipelineElement {
    List<PipelineElement> getElements();
    Stage addElement(PipelineElement element);

    default Stage addStage(String name, Consumer<Stage> stageBuilder) {
        SequenzStage stage = new SequenzStage(name);
        stageBuilder.accept(stage);
        return addElement(stage);
    }

    default Stage addStep(String name, StepFunction function) {
        return addElement(Step.create(name, function));
    }
}
