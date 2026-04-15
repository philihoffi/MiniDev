package org.philipp.fun.minidev.pipeline.impl;

import org.philipp.fun.minidev.pipeline.abstracts.AbstractPipelineElement;
import org.philipp.fun.minidev.pipeline.core.*;
import org.philipp.fun.minidev.pipeline.model.StageResult;
import org.philipp.fun.minidev.pipeline.model.StepResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultStage extends AbstractPipelineElement implements Stage {
    private final List<Step> steps = new ArrayList<>();
    private StageResult cachedResult;

    public DefaultStage(String name) {
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

    @Override
    public StageResult execute(PipelineContext context) {
        if (cachedResult != null) {
            return cachedResult;
        }

        List<PipelineListener> currentListeners = getListeners();
        for (int i = 0; i < steps.size(); i++) {
            Step step = steps.get(i);
            step.setListeners(currentListeners);
            notifyStepStart(step, context);
            try {
                StepResult result = step.execute(context);
                notifyStepEnd(step, context, result);

                if (!result.isSuccess()) {
                    cachedResult = new StageResult(
                            getName(),
                            StageResult.StageStatus.FAILED,
                            "Step failed: " + step.getName(),
                            result
                    );
                    return cachedResult;
                }
            } catch (Exception e) {
                notifyError(step, context, e);
                cachedResult = new StageResult(
                        getName(),
                        StageResult.StageStatus.FAILED,
                        "Exception in step " + step.getName() + ": " + e.getMessage(),
                        null
                );
                return cachedResult;
            }
        }

        cachedResult = new StageResult(getName(), StageResult.StageStatus.SUCCESS, "Stage completed", null);
        return cachedResult;
    }

    @Override
    public StageResult getCachedResult() {
        return cachedResult;
    }
}
