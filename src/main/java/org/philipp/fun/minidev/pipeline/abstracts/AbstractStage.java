package org.philipp.fun.minidev.pipeline.abstracts;

import org.philipp.fun.minidev.pipeline.core.Stage;
import org.philipp.fun.minidev.pipeline.core.Step;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractStage extends AbstractPipelineElement implements Stage {
    private final List<Step> steps = new ArrayList<>();

    protected AbstractStage(String name) {
        super(name);
    }

    @Override
    public List<Step> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    @Override
    public Stage addStep(Step step) {
        if (step == null) {
            throw new IllegalArgumentException("step must not be null");
        }
        steps.add(step);
        return this;
    }
}
