package org.philipp.fun.minidev.pipeline.impl;

import org.philipp.fun.minidev.pipeline.abstracts.AbstractPipelineElement;
import org.philipp.fun.minidev.pipeline.core.*;
import org.philipp.fun.minidev.pipeline.model.PipelineResult;

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
    public PipelineResult execute(PipelineContext context) {
        if (cachedResult != null) {
            return cachedResult;
        }

        List<PipelineListener> currentListeners = getListeners();
        for (int i = 0; i < elements.size(); i++) {
            PipelineElement element = elements.get(i);
            element.setListeners(currentListeners);
            notifyStepStart(element, context);
            try {
                PipelineResult result = element.execute(context);
                notifyStepEnd(element, context, result);

                if (!result.isSuccess()) {
                    cachedResult = PipelineResult.failed(
                            getName(),
                            "Element failed: " + element.getName(),
                            result
                    );
                    return cachedResult;
                }
            } catch (Exception e) {
                notifyError(element, context, e);
                cachedResult = PipelineResult.failed(
                        getName(),
                        "Exception in element " + element.getName() + ": " + e.getMessage(),
                        PipelineResult.failed(element.getName(), e.getMessage())
                );
                return cachedResult;
            }
        }

        cachedResult = PipelineResult.success(getName(), "Completed");
        return cachedResult;
    }

    @Override
    public PipelineResult getCachedResult() {
        return super.getCachedResult();
    }
}
