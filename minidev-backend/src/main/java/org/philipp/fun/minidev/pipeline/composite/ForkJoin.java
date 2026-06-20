package org.philipp.fun.minidev.pipeline.composite;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.philipp.fun.minidev.pipeline.core.BaseElement;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;

/**
 * A pipeline composite that executes a set of fork elements in parallel
 * and then runs a join element.
 */
public class ForkJoin extends BaseElement implements AutoCloseable {

    /** The list of fork elements. */
    private final List<PipelineElement> forks = new ArrayList<>();

    /** The join element to run after all forks complete. */
    private final PipelineElement join;

    /** The executor service for running forks. */
    private final ExecutorService executor;

    /**
     * Constructs a ForkJoin with a virtual-thread-per-task executor.
     *
     * @param name the element name
     * @param join the join element
     */
    public ForkJoin(String name, PipelineElement join) {
        this(name, join, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Constructs a ForkJoin with a custom executor.
     *
     * @param name     the element name
     * @param join     the join element
     * @param executor the executor to run forks
     */
    public ForkJoin(String name, PipelineElement join, ExecutorService executor) {
        super(name);
        this.join = Objects.requireNonNull(join, "join must not be null");
        this.executor = executor;
    }

    /**
     * Adds a fork element.
     *
     * @param element the fork element
     * @return this instance for chaining
     */
    public ForkJoin fork(PipelineElement element) {
        if (element == null) {
            throw new IllegalArgumentException("fork element must not be null");
        }
        forks.add(element);
        return this;
    }

    /**
     * Executes all forks in parallel, then runs the join if all forks succeeded.
     *
     * @param ctx the pipeline context
     * @return {@code true} if all forks and the join succeeded
     * @throws Exception if execution fails
     */
    @Override
    public boolean execute(PipelineContext ctx) throws Exception {
        List<Callable<Boolean>> tasks = forks.stream()
                .map(e -> (Callable<Boolean>) () -> runElement(e, ctx))
                .toList();

        List<Future<Boolean>> futures = executor.invokeAll(tasks);
        boolean allForksOk = true;
        for (var future : futures) {
            try {
                if (!future.get()) {
                    allForksOk = false;
                }
            } catch (Exception e) {
                allForksOk = false;
            }
        }

        if (!allForksOk) {
            return false;
        }

        return runElement(join, ctx);
    }

    /**
     * Shuts down the executor.
     */
    @Override
    public void close() {
        executor.shutdownNow();
    }

    /**
     * Returns an unmodifiable copy of the fork list.
     *
     * @return the fork elements
     */
    public List<PipelineElement> getForks() {
        return List.copyOf(forks);
    }

    /**
     * Returns the join element.
     *
     * @return the join element
     */
    public PipelineElement getJoin() {
        return join;
    }
}
