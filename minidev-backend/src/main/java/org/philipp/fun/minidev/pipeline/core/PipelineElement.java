package org.philipp.fun.minidev.pipeline.core;

import java.util.List;

public interface PipelineElement {
    String getName();

    List<PipelineListener> getListeners();

    void setListeners(List<PipelineListener> listeners);

    boolean execute(PipelineContext context) throws Exception;
}
