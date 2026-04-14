package org.philipp.fun.minidev.pipeline.core;

import org.philipp.fun.minidev.pipeline.impl.LambdaStep;
import org.philipp.fun.minidev.pipeline.impl.DefaultStage;
import org.philipp.fun.minidev.pipeline.model.StageResult;

import java.util.List;

public interface Stage extends PipelineElement {
    static Stage create(String name) {
        return new DefaultStage(name);
    }

    List<Step> getSteps();
    Stage addStep(Step step);

    default Stage addStep(String name, StepFunction function) {
        return addStep(new LambdaStep(name, function));
    }

    StageResult execute(PipelineContext context);
}
