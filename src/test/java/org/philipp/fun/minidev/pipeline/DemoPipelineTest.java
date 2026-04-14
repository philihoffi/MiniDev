package org.philipp.fun.minidev.pipeline;

import org.junit.jupiter.api.Test;
import org.philipp.fun.minidev.pipeline.core.*;
import org.philipp.fun.minidev.pipeline.impl.DefaultPipeline;
import org.philipp.fun.minidev.pipeline.impl.DefaultStage;
import org.philipp.fun.minidev.pipeline.impl.LambdaStep;
import org.philipp.fun.minidev.pipeline.model.StepResult;

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
            public void onStageStart(Stage stage, PipelineContext context) {
                executionOrder.add("START:" + stage.getName());
            }
        });

        // 3. Create initial stage that adds other stages
        Stage triggerStage = new DefaultStage("TriggerStage");
        triggerStage.addStep(new LambdaStep("TriggerStep", context -> {
            // Dynamic addition
            Stage dynamicStage1 = new DefaultStage("DynamicStage1");
            Stage dynamicStage2 = new DefaultStage("DynamicStage2");
            
            context.getPipeline().addStage(dynamicStage1);
            context.getPipeline().addStage(dynamicStage2);
            
            return new StepResult(StepResult.StepStatus.SUCCESS, "Added stages");
        }));

        pipeline.addStage(triggerStage);

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
