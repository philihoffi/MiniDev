package org.philipp.fun.minidev.pipeline.composite;

import org.philipp.fun.minidev.pipeline.core.BaseElement;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ForkJoin extends BaseElement implements AutoCloseable {
    private final List<PipelineElement> forks = new ArrayList<>();
    private final PipelineElement join;
    private final ExecutorService executor;

    public ForkJoin(String name, PipelineElement join) {
        this(name, join, Executors.newVirtualThreadPerTaskExecutor());
    }

    public ForkJoin(String name, PipelineElement join, ExecutorService executor) {
        super(name);
        this.join = Objects.requireNonNull(join, "join must not be null");
        this.executor = executor;
    }

    public ForkJoin fork(PipelineElement element) {
        if (element == null) throw new IllegalArgumentException("fork element must not be null");
        forks.add(element);
        return this;
    }

    @Override
    public boolean execute(PipelineContext ctx) throws Exception {
        List<Callable<Boolean>> tasks = forks.stream()
                .map(e -> (Callable<Boolean>) () -> runElement(e, ctx))
                .toList();

        List<java.util.concurrent.Future<Boolean>> futures = executor.invokeAll(tasks);
        boolean allForksOk = true;
        for (var future : futures) {
            try {
                if (!future.get()) allForksOk = false;
            } catch (Exception e) {
                allForksOk = false;
            }
        }

        if (!allForksOk) return false;

        return runElement(join, ctx);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    public List<PipelineElement> getForks() { return List.copyOf(forks); }
    public PipelineElement getJoin() { return join; }
}