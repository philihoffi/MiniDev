package org.philipp.fun.minidev.pipeline.core;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Abstract base implementation of {@link PipelineElement} providing listener
 * notification and child element execution support.
 */
public abstract class BaseElement implements PipelineElement {

    /** The name of this element. */
    private final String name;

    /** The attached pipeline listeners. */
    private List<PipelineListener> listeners = Collections.emptyList();

    /**
     * Constructs a BaseElement with the given name.
     *
     * @param name the element name, must not be null
     */
    protected BaseElement(String name) {
        this.name = Objects.requireNonNull(name);
    }

    /**
     * Returns the name of this element.
     *
     * @return the element name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns an unmodifiable view of the attached listeners.
     *
     * @return the listener list
     */
    @Override
    public List<PipelineListener> getListeners() {
        return Collections.unmodifiableList(listeners);
    }

    /**
     * Sets the listeners on this element.
     *
     * @param listeners the listener list, or null to clear
     */
    @Override
    public void setListeners(List<PipelineListener> listeners) {
        this.listeners = listeners != null ? listeners : Collections.emptyList();
    }

    /**
     * Notifies all listeners that the given element has started.
     *
     * @param e the element
     * @param ctx the pipeline context
     */
    protected void notifyStart(PipelineElement e, PipelineContext ctx) {
        listeners.forEach(l -> l.onStart(e, ctx));
    }

    /**
     * Notifies all listeners that the given element has ended.
     *
     * @param e the element
     * @param ctx the pipeline context
     * @param s the success flag
     */
    protected void notifyEnd(PipelineElement e, PipelineContext ctx, boolean s) {
        listeners.forEach(l -> l.onEnd(e, ctx, s));
    }

    /**
     * Notifies all listeners of a warning from the given element.
     *
     * @param e the element
     * @param ctx the pipeline context
     * @param m the warning message
     */
    protected void notifyWarning(PipelineElement e, PipelineContext ctx, String m) {
        listeners.forEach(l -> l.onWarning(e, ctx, m));
    }

    /**
     * Notifies all listeners of an error from the given element.
     *
     * @param e the element
     * @param ctx the pipeline context
     * @param ex the exception
     */
    protected void notifyError(PipelineElement e, PipelineContext ctx, Exception ex) {
        listeners.forEach(l -> l.onError(e, ctx, ex));
    }

    /**
     * Runs a child element with listener notifications.
     *
     * @param element the child element to run
     * @param context the pipeline context
     * @return {@code true} if the child executed successfully
     */
    protected boolean runElement(PipelineElement element, PipelineContext context) {
        element.setListeners(this.listeners);
        notifyStart(element, context);
        try {
            boolean result = element.execute(context);
            notifyEnd(element, context, result);
            return result;
        } catch (Exception e) {
            notifyError(element, context, e);
            return false;
        }
    }
}
