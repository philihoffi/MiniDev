package org.philipp.fun.minidev.pipeline.composite;

import org.philipp.fun.minidev.pipeline.core.BaseElement;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CircuitBreaker extends BaseElement {
    private final PipelineElement element;
    private final int failureThreshold;
    private final Duration resetTimeout;

    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicReference<CircuitState> state = new AtomicReference<>(CircuitState.CLOSED);
    private final AtomicReference<Instant> lastFailureTime = new AtomicReference<>(Instant.MIN);

    public CircuitBreaker(String name, PipelineElement element, int failureThreshold, Duration resetTimeout) {
        super(name);
        this.element = Objects.requireNonNull(element, "element must not be null");
        this.failureThreshold = failureThreshold;
        this.resetTimeout = Objects.requireNonNull(resetTimeout, "resetTimeout must not be null");
    }

    @Override
    public boolean execute(PipelineContext ctx) throws Exception {
        CircuitState currentState = state.get();

        if (currentState == CircuitState.OPEN) {
            if (Duration.between(lastFailureTime.get(), Instant.now()).compareTo(resetTimeout) >= 0) {
                if (!state.compareAndSet(CircuitState.OPEN, CircuitState.HALF_OPEN)) {
                    return execute(ctx);
                }
            } else {
                notifyWarning(this, ctx, "Circuit breaker OPEN for " + getName() + ", skipping execution");
                return false;
            }
        }

        boolean success = runElement(element, ctx);

        if (success) {
            if (state.compareAndSet(CircuitState.HALF_OPEN, CircuitState.CLOSED)) {
                failureCount.set(0);
            } else if (state.get() == CircuitState.CLOSED) {
                failureCount.set(0);
            }
        } else {
            failureCount.incrementAndGet();
            lastFailureTime.set(Instant.now());
            if (failureCount.get() >= failureThreshold) {
                state.set(CircuitState.OPEN);
                notifyWarning(this, ctx, "Circuit breaker OPENED for " + getName()
                        + " after " + failureCount.get() + " failures");
            }
        }

        return success;
    }

    public PipelineElement getElement() { return element; }
    public int getFailureThreshold() { return failureThreshold; }
    public Duration getResetTimeout() { return resetTimeout; }
    public CircuitState getState() { return state.get(); }
    public int getFailureCount() { return failureCount.get(); }

    public enum CircuitState {
        CLOSED, OPEN, HALF_OPEN
    }
}