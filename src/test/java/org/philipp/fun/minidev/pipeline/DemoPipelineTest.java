package org.philipp.fun.minidev.pipeline;

import org.junit.jupiter.api.Test;
import org.philipp.fun.minidev.pipeline.core.*;
import org.philipp.fun.minidev.pipeline.impl.SequenzStage;
import org.philipp.fun.minidev.pipeline.impl.LambdaStep;
import org.philipp.fun.minidev.pipeline.model.PipelineResult;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DemoPipelineTest {

    @Test
    void testDynamicStageAddition() {
        // 1. Setup Pipeline
        Pipeline pipeline = Pipeline.create("DemoPipeline");
        List<String> executionOrder = new ArrayList<>();

        // 2. Add a Listener
        pipeline.addListener(new PipelineListener() {
            @Override
            public void onStepStart(PipelineElement stage, PipelineContext context) {
                if (stage instanceof Stage) {
                    executionOrder.add("START:" + stage.getName());
                }
            }
        });

        // 3. Create initial stage that adds other stages
        Stage triggerStage = new SequenzStage("TriggerStage");
        triggerStage.addElement(new LambdaStep("TriggerStep", context -> {
            // Dynamic addition
            Stage dynamicStage1 = new SequenzStage("DynamicStage1");
            Stage dynamicStage2 = new SequenzStage("DynamicStage2");
            
            context.getPipeline().addElement(dynamicStage1);
            context.getPipeline().addElement(dynamicStage2);
            
            return new PipelineResult("TriggerStep", PipelineResult.Status.SUCCESS, "Added stages", null);
        }));

        pipeline.addElement(triggerStage);

        // 4. Execute
        pipeline.execute();

        // 5. Verify
        assertThat(executionOrder).containsExactly(
                "START:TriggerStage",
                "START:DynamicStage1",
                "START:DynamicStage2"
        );
    }
}
