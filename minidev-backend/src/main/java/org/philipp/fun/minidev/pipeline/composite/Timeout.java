package org.philipp.fun.minidev.pipeline.composite;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.philipp.fun.minidev.pipeline.core.BaseElement;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;

/**
 * A pipeline composite that wraps a child element with a timeout.
 */
public class Timeout extends BaseElement {

    /** Shared virtual-thread executor for timeout tasks. */
    private static final ExecutorService SHARED_EXECUTOR = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("timeout-", 0).factory()
    );

    /** The wrapped child element. */
    private final PipelineElement element;

    /** The maximum duration before timeout. */
    private final Duration timeout;

    /**
     * Constructs a Timeout element.
     *
     * @param name    the element name
     * @param element the child element to wrap
     * @param timeout the maximum duration to wait
     */
    public Timeout(String name, PipelineElement element, Duration timeout) {
        super(name);
        this.element = Objects.requireNonNull(element, "element must not be null");
        this.timeout = Objects.requireNonNull(timeout, "timeout must not be null");
    }

    /**
     * Executes the child element with a timeout.
     *
     * @param ctx the pipeline context
     * @return {@code true} if the child completed within the timeout and
     *         succeeded
     * @throws Exception if execution fails with a non-timeout error
     */
    @Override
    public boolean execute(PipelineContext ctx) throws Exception {
        Callable<Boolean> task = () -> runElement(element, ctx);
        var future = SHARED_EXECUTOR.submit(task);
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            notifyWarning(this, ctx, "Timeout after " + timeout.toMillis() + "ms for " + getName());
            return false;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception ex) {
                throw ex;
            }
            throw new Exception("Timeout element execution failed", cause);
        }
    }

    /**
     * Returns the wrapped child element.
     *
     * @return the child element
     */
    public PipelineElement getElement() {
        return element;
    }

    /**
     * Returns the timeout duration.
     *
     * @return the timeout
     */
    public Duration getTimeout() {
        return timeout;
    }
}
