package org.philipp.fun.minidev.pipeline.impl;

import org.philipp.fun.minidev.pipeline.abstracts.AbstractPipeline;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.Stage;
import org.philipp.fun.minidev.pipeline.model.PipelineResult;
import org.philipp.fun.minidev.pipeline.model.StageResult;

public class DefaultPipeline extends AbstractPipeline {

    public DefaultPipeline(String name) {
        super(name);
    }

    @Override
    public PipelineResult execute(PipelineContext context) {
        context.setPipeline(this);
        notifyPipelineStart(context);
        PipelineResult finalResult = null;

        try {
            for (int i = 0; i < stages.size(); i++) {
                Stage stage = stages.get(i);
                notifyStageStart(stage, context);
                if (stage instanceof DefaultStage defaultStage) {
                    defaultStage.setListeners(getListeners());
                }
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
