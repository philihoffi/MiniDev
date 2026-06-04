package org.philipp.fun.minidev.pipeline;

import java.util.ArrayList;
import java.util.List;

public class Sequence extends BaseElement {
    protected final List<PipelineElement> elements = new ArrayList<>();

    public Sequence(String name) { super(name); }

    public Sequence add(PipelineElement e) {
        if (e == null) {
            throw new IllegalArgumentException("element must not be null");
        }
        elements.add(e);
        return this;
    }

    @Override
    public boolean execute(PipelineContext ctx) {
        for (PipelineElement e : elements) {
            if (!runElement(e, ctx)) return false;
        }
        return true;
    }

    public List<PipelineElement> getElements() { return elements; }
}
