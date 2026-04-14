package org.philipp.fun.minidev.pipeline.abstracts;

import org.philipp.fun.minidev.pipeline.core.*;
import org.philipp.fun.minidev.pipeline.model.PipelineResult;
import org.philipp.fun.minidev.pipeline.model.StageResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractPipeline extends AbstractPipelineElement implements Pipeline {
    protected final List<Stage> stages = new ArrayList<>();
    private final List<PipelineListener> listeners = new ArrayList<>();

    protected AbstractPipeline(String name) {
        super(name);
    }

    @Override
    public List<Stage> getStages() {
        return Collections.unmodifiableList(stages);
    }

    @Override
    public Pipeline addStage(Stage stage) {
        if (stage == null) {
            throw new IllegalArgumentException("stage must not be null");
        }
        stages.add(stage);
        return this;
    }

    @Override
    public Pipeline addListener(PipelineListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
        return this;
    }

    @Override
    public List<PipelineListener> getListeners() {
        return Collections.unmodifiableList(listeners);
    }

    protected void notifyPipelineStart(PipelineContext context) {
        listeners.forEach(l -> l.onPipelineStart(this, context));
    }

    protected void notifyPipelineEnd(PipelineContext context, PipelineResult result) {
        listeners.forEach(l -> l.onPipelineEnd(this, context, result));
    }

    protected void notifyStageStart(Stage stage, PipelineContext context) {
        listeners.forEach(l -> l.onStageStart(stage, context));
    }

    protected void notifyStageEnd(Stage stage, PipelineContext context, StageResult result) {
        listeners.forEach(l -> l.onStageEnd(stage, context, result));
    }

    protected void notifyError(PipelineElement element, PipelineContext context, Exception e) {
        listeners.forEach(l -> l.onError(element, context, e));
    }
}
