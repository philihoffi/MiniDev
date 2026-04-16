package org.philipp.fun.minidev.pipeline.impl;

import org.philipp.fun.minidev.pipeline.abstracts.AbstractPipelineElement;
import org.philipp.fun.minidev.pipeline.core.*;
import org.philipp.fun.minidev.pipeline.model.PipelineResult;
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
    public PipelineResult execute(PipelineContext context) {
        if (cachedResult != null) {
            return cachedResult;
        }

        List<PipelineListener> currentListeners = getListeners();

        List<CompletableFuture<PipelineResult>> futures = elements.stream()
                .map(element -> CompletableFuture.supplyAsync(() -> {
                    element.setListeners(currentListeners);
                    notifyStepStart(element, context);
                    try {
                        PipelineResult result = element.execute(context);
                        notifyStepEnd(element, context, result);
                        return result;
                    } catch (Exception e) {
                        notifyError(element, context, e);
                        return PipelineResult.failed(element.getName(), "Exception: " + e.getMessage());
                    }
                }, executor))
                .toList();

        for (CompletableFuture<PipelineResult> future : futures) {
            try {
                PipelineResult result = future.get();
                if (!result.isSuccess()) {
                    cachedResult = PipelineResult.failed(
                            getName(),
                            "Element failed: " + result.name(),
                            result
                    );
                    return cachedResult;
                }
            } catch (Exception e) {
                cachedResult = PipelineResult.failed(getName(), "Thread interruption: " + e.getMessage());
                return cachedResult;
            }
        }

        cachedResult = PipelineResult.success(getName(), "All parallel elements completed successfully");
        return cachedResult;
    }

    @Override
    public PipelineResult getCachedResult() {
        return super.getCachedResult();
    }
}
