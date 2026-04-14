package org.philipp.fun.minidev.pipeline.impl;

import org.philipp.fun.minidev.pipeline.abstracts.AbstractStage;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.Step;
import org.philipp.fun.minidev.pipeline.model.StageResult;
import org.philipp.fun.minidev.pipeline.model.StepResult;

import org.philipp.fun.minidev.pipeline.core.PipelineListener;

import java.util.Collections;
import java.util.List;

public class DefaultStage extends AbstractStage {
    private List<PipelineListener> listeners = Collections.emptyList();

    public DefaultStage(String name) {
        super(name);
    }

    public void setListeners(List<PipelineListener> listeners) {
        this.listeners = listeners != null ? listeners : Collections.emptyList();
    }

    @Override
    public List<PipelineListener> getListeners() {
        return Collections.unmodifiableList(listeners);
    }

    @Override
    public StageResult execute(PipelineContext context) {
        for (Step step : getSteps()) {
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

    private void notifyStepStart(Step step, PipelineContext context) {
        listeners.forEach(l -> l.onStepStart(step, context));
    }

    private void notifyStepEnd(Step step, PipelineContext context, StepResult result) {
        listeners.forEach(l -> l.onStepEnd(step, context, result));
    }

    private void notifyError(Step step, PipelineContext context, Exception e) {
        listeners.forEach(l -> l.onError(step, context, e));
    }
}
