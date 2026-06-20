package org.philipp.fun.minidev.pipeline.composite;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.philipp.fun.minidev.pipeline.core.BaseElement;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;

/**
 * A pipeline composite that executes all child elements in parallel.
 */
public class Parallel extends BaseElement implements AutoCloseable {

    /** The child elements to execute in parallel. */
    private final List<PipelineElement> elements = new ArrayList<>();

    /** The executor service for parallel execution. */
    private final ExecutorService executor;

    /**
     * Constructs a Parallel element with a virtual-thread-per-task executor.
     *
     * @param name the element name
     */
    public Parallel(String name) {
        this(name, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Constructs a Parallel element with a custom executor.
     *
     * @param name     the element name
     * @param executor the executor to use
     */
    public Parallel(String name, ExecutorService executor) {
        super(name);
        this.executor = executor;
    }

    /**
     * Adds a child element to execute in parallel.
     *
     * @param element the child element
     * @return this instance for chaining
     */
    public Parallel add(PipelineElement element) {
        if (element == null) {
            throw new IllegalArgumentException("element must not be null");
        }
        elements.add(element);
        return this;
    }

    /**
     * Executes all child elements in parallel and returns whether all succeeded.
     *
     * @param ctx the pipeline context
     * @return {@code true} if all children succeeded
     * @throws Exception if execution fails
     */
    @Override
    public boolean execute(PipelineContext ctx) throws Exception {
        if (elements.isEmpty()) {
            return true;
        }

        List<Callable<Boolean>> tasks = elements.stream()
                .map(e -> (Callable<Boolean>) () -> runElement(e, ctx))
                .toList();

        List<Future<Boolean>> futures = executor.invokeAll(tasks);
        boolean allSuccess = true;
        for (var future : futures) {
            try {
                if (!future.get()) {
                    allSuccess = false;
                }
            } catch (Exception e) {
                allSuccess = false;
            }
        }
        return allSuccess;
    }

    /**
     * Shuts down the executor.
     */
    @Override
    public void close() {
        executor.shutdownNow();
    }

    /**
     * Returns an unmodifiable copy of the child elements.
     *
     * @return the child elements
     */
    public List<PipelineElement> getElements() {
        return List.copyOf(elements);
    }
}
