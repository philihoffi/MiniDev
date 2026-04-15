package org.philipp.fun.minidev.pipeline.abstracts;

import org.philipp.fun.minidev.pipeline.core.*;
import org.philipp.fun.minidev.pipeline.model.PipelineResult;
import org.philipp.fun.minidev.pipeline.model.StageResult;
import org.philipp.fun.minidev.pipeline.model.StepResult;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class AbstractPipelineElement implements PipelineElement {
    private final String name;
    private List<PipelineListener> listeners = Collections.emptyList();

    protected AbstractPipelineElement(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<PipelineListener> getListeners() {
        return Collections.unmodifiableList(listeners);
    }

    @Override
    public void setListeners(List<PipelineListener> listeners) {
        this.listeners = listeners != null ? listeners : Collections.emptyList();
    }

    protected void notifyPipelineStart(PipelineContext context) {
        if (this instanceof Pipeline pipeline) {
            getListeners().forEach(l -> l.onPipelineStart(pipeline, context));
        }
    }

    protected void notifyPipelineEnd(PipelineContext context, PipelineResult result) {
        if (this instanceof Pipeline pipeline) {
            getListeners().forEach(l -> l.onPipelineEnd(pipeline, context, result));
        }
    }

    protected void notifyStageStart(Stage stage, PipelineContext context) {
        getListeners().forEach(l -> l.onStageStart(stage, context));
    }

    protected void notifyStageEnd(Stage stage, PipelineContext context, StageResult result) {
        getListeners().forEach(l -> l.onStageEnd(stage, context, result));
    }

    protected void notifyStepStart(Step step, PipelineContext context) {
        getListeners().forEach(l -> l.onStepStart(step, context));
    }

    protected void notifyStepEnd(Step step, PipelineContext context, StepResult result) {
        getListeners().forEach(l -> l.onStepEnd(step, context, result));
    }

    protected void notifyWarning(PipelineElement element, PipelineContext context, String message) {
        getListeners().forEach(l -> l.onWarning(element, context, message));
    }

    protected void notifyError(PipelineElement element, PipelineContext context, Exception e) {
        getListeners().forEach(l -> l.onError(element, context, e));
    }
}
