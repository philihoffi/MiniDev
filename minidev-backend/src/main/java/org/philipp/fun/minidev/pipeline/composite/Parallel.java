package org.philipp.fun.minidev.pipeline.composite;

import org.philipp.fun.minidev.pipeline.core.BaseElement;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Parallel extends BaseElement {
    private final List<PipelineElement> elements = new ArrayList<>();
    private final ExecutorService executor;

    public Parallel(String name) {
        this(name, Executors.newVirtualThreadPerTaskExecutor());
    }

    public Parallel(String name, ExecutorService executor) {
        super(name);
        this.executor = executor;
    }

    public Parallel add(PipelineElement element) {
        if (element == null) throw new IllegalArgumentException("element must not be null");
        elements.add(element);
        return this;
    }

    @Override
    public boolean execute(PipelineContext ctx) throws Exception {
        if (elements.isEmpty()) return true;

        List<Callable<Boolean>> tasks = elements.stream()
                .map(e -> (Callable<Boolean>) () -> runElement(e, ctx))
                .toList();

        List<java.util.concurrent.Future<Boolean>> futures = executor.invokeAll(tasks);
        boolean allSuccess = true;
        for (var future : futures) {
            try {
                if (!future.get()) allSuccess = false;
            } catch (Exception e) {
                allSuccess = false;
            }
        }
        return allSuccess;
    }

    public List<PipelineElement> getElements() { return List.copyOf(elements); }
}