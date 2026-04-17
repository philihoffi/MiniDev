package org.philipp.fun.minidev.pipeline.impl;

import org.philipp.fun.minidev.pipeline.abstracts.AbstractPipelineElement;
import org.philipp.fun.minidev.pipeline.core.*;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class ParallelStage extends AbstractPipelineElement implements Stage {
    private final List<PipelineElement> elements = new ArrayList<>();

    private final Executor executor;

    public ParallelStage(@Qualifier("globalExecutor") Executor executor,String name) {
        super(name);
        this.executor = executor;
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

        List<CompletableFuture<Boolean>> futures = elements.stream()
                .map(element -> CompletableFuture.supplyAsync(() -> {
                    element.setListeners(currentListeners);
                    notifyStepStart(element, context);
                    try {
                        boolean result = element.execute(context);
                        notifyStepEnd(element, context, result);
                        return result;
                    } catch (Exception e) {
                        notifyError(element, context, e);
                        return false;
                    }
                }, executor))
                .toList();

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            notifyError(this, context, e);
            return false;
        }

        for (CompletableFuture<Boolean> future : futures) {
            if (!future.join()) {
                return false;
            }
        }
        return true;
    }
}
