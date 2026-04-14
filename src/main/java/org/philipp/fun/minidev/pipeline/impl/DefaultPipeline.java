package org.philipp.fun.minidev.pipeline.impl;

import org.philipp.fun.minidev.pipeline.abstracts.AbstractPipelineElement;
import org.philipp.fun.minidev.pipeline.core.*;
import org.philipp.fun.minidev.pipeline.model.PipelineResult;
import org.philipp.fun.minidev.pipeline.model.StageResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultPipeline extends AbstractPipelineElement implements Pipeline {
    protected final List<Stage> stages = new ArrayList<>();

    public DefaultPipeline(String name) {
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

    @Override
    public PipelineResult execute(PipelineContext context) {
        context.setPipeline(this);
        notifyPipelineStart(context);
        PipelineResult finalResult = null;

        try {
            for (int i = 0; i < stages.size(); i++) {
                Stage stage = stages.get(i);
                stage.setListeners(getListeners());
                notifyStageStart(stage, context);
                try {
                    StageResult stageResult = stage.execute(context);
                    notifyStageEnd(stage, context, stageResult);

                    if (!stageResult.isSuccess()) {
                        finalResult = new PipelineResult(
                                getName(),
                                PipelineResult.PipelineStatus.FAILED,
                                "Pipeline failed in stage: " + stage.getName(),
                                stageResult
                        );
                        break;
                    }
                } catch (Exception e) {
                    notifyError(stage, context, e);
                    finalResult = new PipelineResult(
                            getName(),
                            PipelineResult.PipelineStatus.FAILED,
                            "Unexpected exception in stage " + stage.getName() + ": " + e.getMessage(),
                            new StageResult(stage.getName(), StageResult.StageStatus.FAILED, e.getMessage(), null)
                    );
                    break;
                }
            }

            if (finalResult == null) {
                finalResult = new PipelineResult(
                        getName(),
                        PipelineResult.PipelineStatus.SUCCESS,
                        "Pipeline completed successfully",
                        null
                );
            }
        } finally {
            notifyPipelineEnd(context, finalResult);
        }

        return finalResult;
    }
}
