package org.philipp.fun.minidev.pipeline.abstracts;

import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.Step;

public abstract class AbstractStep extends AbstractPipelineElement implements Step {

    protected AbstractStep(String name) {
        super(name);
    }
}
