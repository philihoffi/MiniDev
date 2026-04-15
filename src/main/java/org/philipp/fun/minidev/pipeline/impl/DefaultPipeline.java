package org.philipp.fun.minidev.pipeline.impl;

import org.philipp.fun.minidev.pipeline.abstracts.AbstractPipelineElement;
import org.philipp.fun.minidev.pipeline.core.*;
import org.philipp.fun.minidev.pipeline.model.PipelineResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class DefaultPipeline extends SequenzStage implements Pipeline {
    public DefaultPipeline(String name) {
        super(name);
    }

    @Override
    public Pipeline addStage(String name, Consumer<Stage> stageBuilder) {
        super.addStage(name, stageBuilder);
        return this;
    }

    @Override
    public Pipeline addStep(String name, StepFunction function) {
        super.addStep(name, function);
        return this;
    }

    @Override
    public Pipeline addElement(PipelineElement element) {
        super.addElement(element);
        return this;
    }

    @Override
    public Pipeline addListener(PipelineListener listener) {
        if (listener != null) {
            List<PipelineListener> current = new ArrayList<>(getListeners());
            current.add(listener);
            setListeners(current);
        }
        return this;
    }

    @Override
    public PipelineResult execute(PipelineContext context) {
        if (getCachedResult() != null) {
            return getCachedResult();
        }
        context.setPipeline(this);
        getListeners().forEach(l -> l.onPipelineStart(this, context));
        PipelineResult result = null;
        try {
            result = super.execute(context);
        } finally {
            PipelineResult finalResult = result;
            getListeners().forEach(l -> l.onPipelineEnd(this, context, finalResult));
        }
        return result;
    }
}
