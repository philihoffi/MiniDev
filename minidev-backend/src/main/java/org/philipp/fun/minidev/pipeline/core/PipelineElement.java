package org.philipp.fun.minidev.pipeline.core;

import java.util.List;

/**
 * Interface for all pipeline elements that can be executed within a pipeline context.
 */
public interface PipelineElement {

    /**
     * Returns the name of this pipeline element.
     *
     * @return the element name
     */
    String getName();

    /**
     * Returns the list of attached pipeline listeners.
     *
     * @return an unmodifiable list of listeners
     */
    List<PipelineListener> getListeners();

    /**
     * Sets the list of pipeline listeners on this element.
     *
     * @param listeners the listener list to attach
     */
    void setListeners(List<PipelineListener> listeners);

    /**
     * Executes this pipeline element with the given context.
     *
     * @param context the pipeline context
     * @return {@code true} if execution succeeded, {@code false} otherwise
     * @throws Exception if an error occurs during execution
     */
    boolean execute(PipelineContext context) throws Exception;
}
