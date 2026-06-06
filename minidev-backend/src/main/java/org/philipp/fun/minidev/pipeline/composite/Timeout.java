package org.philipp.fun.minidev.pipeline.composite;

import org.philipp.fun.minidev.pipeline.core.BaseElement;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Timeout extends BaseElement {
    private static final ExecutorService SHARED_EXECUTOR = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("timeout-", 0).factory()
    );

    private final PipelineElement element;
    private final Duration timeout;

    public Timeout(String name, PipelineElement element, Duration timeout) {
        super(name);
        this.element = Objects.requireNonNull(element, "element must not be null");
        this.timeout = Objects.requireNonNull(timeout, "timeout must not be null");
    }

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
            if (cause instanceof Exception ex) throw ex;
            throw new Exception("Timeout element execution failed", cause);
        }
    }

    public PipelineElement getElement() { return element; }
    public Duration getTimeout() { return timeout; }
}