package org.philipp.fun.minidev.pipeline.composite;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.philipp.fun.minidev.pipeline.core.BaseElement;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;

/**
 * A circuit breaker pipeline element that stops execution after a configurable
 * number of consecutive failures and periodically retries.
 */
public class CircuitBreaker extends BaseElement {

    /** The wrapped element. */
    private final PipelineElement element;

    /** The number of failures before opening the circuit. */
    private final int failureThreshold;

    /** The duration to wait before transitioning to half-open. */
    private final Duration resetTimeout;

    /** Current consecutive failure count. */
    private final AtomicInteger failureCount = new AtomicInteger(0);

    /** Current circuit breaker state. */
    private final AtomicReference<CircuitState> state =
            new AtomicReference<>(CircuitState.CLOSED);

    /** Timestamp of the last failure. */
    private final AtomicReference<Instant> lastFailureTime =
            new AtomicReference<>(Instant.MIN);

    /**
     * Constructs a new CircuitBreaker.
     *
     * @param name             the element name
     * @param element          the wrapped element
     * @param failureThreshold the failure threshold
     * @param resetTimeout     the reset timeout
     */
    public CircuitBreaker(
            String name,
            PipelineElement element,
            int failureThreshold,
            Duration resetTimeout) {
        super(name);
        this.element = Objects.requireNonNull(
                element, "element must not be null");
        this.failureThreshold = failureThreshold;
        this.resetTimeout = Objects.requireNonNull(
                resetTimeout, "resetTimeout must not be null");
    }

    @Override
    public boolean execute(PipelineContext ctx) throws Exception {
        CircuitState currentState = state.get();

        if (currentState == CircuitState.OPEN) {
            if (Duration.between(lastFailureTime.get(), Instant.now())
                    .compareTo(resetTimeout) >= 0) {
                if (!state.compareAndSet(
                        CircuitState.OPEN, CircuitState.HALF_OPEN)) {
                    return execute(ctx);
                }
            } else {
                notifyWarning(
                        this,
                        ctx,
                        "Circuit breaker OPEN for "
                        + getName() + ", skipping execution");
                return false;
            }
        }

        boolean success = runElement(element, ctx);

        if (success) {
            if (state.compareAndSet(
                    CircuitState.HALF_OPEN, CircuitState.CLOSED)) {
                failureCount.set(0);
            } else if (state.get() == CircuitState.CLOSED) {
                failureCount.set(0);
            }
        } else {
            failureCount.incrementAndGet();
            lastFailureTime.set(Instant.now());
            if (failureCount.get() >= failureThreshold) {
                state.set(CircuitState.OPEN);
                notifyWarning(
                        this,
                        ctx,
                        "Circuit breaker OPENED for " + getName()
                        + " after " + failureCount.get() + " failures");
            }
        }

        return success;
    }

    /**
     * Gets the wrapped element.
     *
     * @return the element
     */
    public PipelineElement getElement() {
        return element;
    }

    /**
     * Gets the failure threshold.
     *
     * @return the failure threshold
     */
    public int getFailureThreshold() {
        return failureThreshold;
    }

    /**
     * Gets the reset timeout.
     *
     * @return the reset timeout
     */
    public Duration getResetTimeout() {
        return resetTimeout;
    }

    /**
     * Gets the current circuit state.
     *
     * @return the circuit state
     */
    public CircuitState getState() {
        return state.get();
    }

    /**
     * Gets the current failure count.
     *
     * @return the failure count
     */
    public int getFailureCount() {
        return failureCount.get();
    }

    /**
     * Possible states for the circuit breaker.
     */
    public enum CircuitState {
        /** Normal operation - requests pass through. */
        CLOSED,
        /** Failure threshold exceeded - requests are blocked. */
        OPEN,
        /** Probing state - single request allowed to test recovery. */
        HALF_OPEN
    }
}