package org.philipp.fun.minidev.pipeline.impl;

import org.philipp.fun.minidev.pipeline.abstracts.AbstractStage;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineListener;
import org.philipp.fun.minidev.pipeline.core.Step;
import org.philipp.fun.minidev.pipeline.model.StageResult;
import org.philipp.fun.minidev.pipeline.model.StepResult;

import java.util.List;

public class DefaultStage extends AbstractStage {

    public DefaultStage(String name) {
        super(name);
    }

    @Override
    public StageResult execute(PipelineContext context) {
        List<PipelineListener> currentListeners = getListeners();
        for (Step step : getSteps()) {
            step.setListeners(currentListeners);
            notifyStepStart(step, context);
            try {
                StepResult result = step.execute(context);
                notifyStepEnd(step, context, result);

                if (!result.isSuccess()) {
                    return new StageResult(
                            getName(),
                            StageResult.StageStatus.FAILED,
                            "Step failed: " + step.getName(),
                            result
                    );
                }
            } catch (Exception e) {
                notifyError(step, context, e);
                return new StageResult(
                        getName(),
                        StageResult.StageStatus.FAILED,
                        "Exception in step " + step.getName() + ": " + e.getMessage(),
                        null
                );
            }
        }

        return new StageResult(getName(), StageResult.StageStatus.SUCCESS, "Stage completed", null);
    }
}
