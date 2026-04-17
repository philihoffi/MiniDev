package org.philipp.fun.minidev.pipeline.impl;

import org.philipp.fun.minidev.pipeline.abstracts.AbstractPipelineElement;
import org.philipp.fun.minidev.pipeline.core.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SequenzStage extends AbstractPipelineElement implements Stage {
    private final List<PipelineElement> elements = new ArrayList<>();

    public SequenzStage(String name) {
        super(name);
    }

    @Override
    public List<PipelineElement> getElements() {
        return Collections.unmodifiableList(elements);
    }

    @Override
    public Stage addElement(PipelineElement element) {
        if (element == null) {
            throw new IllegalArgumentException("element must not be null");
        }
        elements.add(element);
        return this;
    }

    @Override
    public boolean execute(PipelineContext context) {

        List<PipelineListener> currentListeners = getListeners();
        for (PipelineElement element : elements) {
            element.setListeners(currentListeners);
            notifyStepStart(element, context);
            try {
                boolean result = element.execute(context);
                notifyStepEnd(element, context, result);
                if (!result) return false;
            } catch (Exception e) {
                notifyError(element, context, e);
                return false;
            }
        }
        return true;
    }
}
