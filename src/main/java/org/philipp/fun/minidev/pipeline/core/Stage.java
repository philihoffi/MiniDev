package org.philipp.fun.minidev.pipeline.core;

import java.util.List;

public interface Stage extends PipelineElement {
    List<PipelineElement> getElements();
    Stage addElement(PipelineElement element);

    default Stage addStep(String name, StepFunction function) {
        return addElement(Step.create(name, function));
    }
}
