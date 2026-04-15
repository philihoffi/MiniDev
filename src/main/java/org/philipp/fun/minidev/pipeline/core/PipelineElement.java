package org.philipp.fun.minidev.pipeline.core;

import org.philipp.fun.minidev.pipeline.model.PipelineResult;

import java.util.Collections;
import java.util.List;

public interface PipelineElement {
    String getName();

    List<PipelineListener> getListeners();

    void setListeners(List<PipelineListener> listeners);

    PipelineResult execute(PipelineContext context);

    PipelineResult getCachedResult();
}
