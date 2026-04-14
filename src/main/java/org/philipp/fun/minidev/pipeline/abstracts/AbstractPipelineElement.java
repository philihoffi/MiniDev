package org.philipp.fun.minidev.pipeline.abstracts;

import org.philipp.fun.minidev.pipeline.core.PipelineElement;

import java.util.Objects;

public abstract class AbstractPipelineElement implements PipelineElement {
    private final String name;

    protected AbstractPipelineElement(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    @Override
    public String getName() {
        return name;
    }
}
