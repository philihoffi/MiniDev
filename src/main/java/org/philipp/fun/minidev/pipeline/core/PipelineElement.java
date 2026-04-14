package org.philipp.fun.minidev.pipeline.core;

import java.util.Collections;
import java.util.List;

public interface PipelineElement {
    String getName();

    default List<PipelineListener> getListeners() {
        return Collections.emptyList();
    }
}
