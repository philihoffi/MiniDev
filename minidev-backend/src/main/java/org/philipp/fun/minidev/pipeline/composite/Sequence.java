package org.philipp.fun.minidev.pipeline.composite;

import java.util.ArrayList;
import java.util.List;

import org.philipp.fun.minidev.pipeline.core.BaseElement;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;

/**
 * A composite pipeline element that executes its child elements in sequence.
 */
public class Sequence extends BaseElement {

    /** Ordered list of child pipeline elements to execute. */
    protected final List<PipelineElement> elements = new ArrayList<>();

    /**
     * Constructs a {@code Sequence} with the given name.
     *
     * @param name the name of this sequence
     */
    public Sequence(String name) {
        super(name);
    }

    /**
     * Appends a child element to this sequence.
     *
     * @param e the element to add (must not be null)
     * @return this sequence for method chaining
     * @throws IllegalArgumentException if the element is null
     */
    public Sequence add(PipelineElement e) {
        if (e == null) {
            throw new IllegalArgumentException("element must not be null");
        }
        elements.add(e);
        return this;
    }

    /**
     * Executes all child elements in order. If any element fails, execution
     * stops immediately.
     *
     * @param ctx the pipeline context
     * @return {@code true} if all elements succeeded, {@code false} otherwise
     */
    @Override
    public boolean execute(PipelineContext ctx) {
        for (PipelineElement e : elements) {
            if (!runElement(e, ctx)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the list of child elements.
     *
     * @return the list of child pipeline elements
     */
    public List<PipelineElement> getElements() {
        return elements;
    }
}