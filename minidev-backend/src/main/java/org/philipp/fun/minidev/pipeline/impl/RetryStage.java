package org.philipp.fun.minidev.pipeline.impl;

import org.philipp.fun.minidev.pipeline.abstracts.AbstractPipelineElement;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;
import org.philipp.fun.minidev.pipeline.core.PipelineListener;
import org.philipp.fun.minidev.pipeline.core.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RetryStage extends AbstractPipelineElement implements Stage {
    private final List<PipelineElement> elements = new ArrayList<>();
    private final int maxRetries;

    public RetryStage(String name, int maxRetries) {
        super(name);
        this.maxRetries = maxRetries;
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
    public boolean execute(PipelineContext context) throws Exception {
        int attempt = 0;
        List<PipelineListener> currentListeners = getListeners();

        while (attempt <= maxRetries) {
            boolean sequenceSuccess = true;
            for (PipelineElement element : elements) {
                element.setListeners(currentListeners);
                notifyStepStart(element, context);
                try {
                    boolean result = element.execute(context);
                    notifyStepEnd(element, context, result);
                    if (!result) {
                        sequenceSuccess = false;
                        break;
                    }
                } catch (Exception e) {
                    notifyError(element, context, e);
                    sequenceSuccess = false;
                    break;
                }
            }

            if (sequenceSuccess) {
                return true;
            }

            attempt++;
            if (attempt <= maxRetries) {
                notifyWarning(this, context, "Retry attempt " + attempt + " for " + getName());
            }
        }
        return false;
    }
}
