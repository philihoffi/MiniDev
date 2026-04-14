package org.philipp.fun.minidev.pipeline.abstracts;

import org.philipp.fun.minidev.pipeline.core.*;
import org.philipp.fun.minidev.pipeline.model.PipelineResult;
import org.philipp.fun.minidev.pipeline.model.StageResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractPipeline extends AbstractPipelineElement implements Pipeline {
    protected final List<Stage> stages = new ArrayList<>();

    protected AbstractPipeline(String name) {
        super(name);
    }

    @Override
    public List<Stage> getStages() {
        return Collections.unmodifiableList(stages);
    }

    @Override
    public Pipeline addStage(Stage stage) {
        if (stage == null) {
            throw new IllegalArgumentException("stage must not be null");
        }
        stages.add(stage);
        return this;
    }

    @Override
    public Pipeline addListener(PipelineListener listener) {
        if (listener != null) {
            List<PipelineListener> current = new ArrayList<>(getListeners());
            current.add(listener);
            setListeners(current);
        }
        return this;
    }
}
