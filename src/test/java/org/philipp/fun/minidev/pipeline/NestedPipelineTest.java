package org.philipp.fun.minidev.pipeline;

import org.junit.jupiter.api.Test;
import org.philipp.fun.minidev.pipeline.core.*;
import org.philipp.fun.minidev.pipeline.model.PipelineResult;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NestedPipelineTest {

    @Test
    public void testDeeplyNestedPipeline() {
        List<String> executionOrder = new ArrayList<>();

        Pipeline pipeline = Pipeline.create("DeeplyNested")
            .addStep("Step 1", context -> {
                executionOrder.add("Step 1");
                return PipelineResult.success("Step 1 done");
            })
            .addStage("Level 1 Stage", stage1 -> {
                stage1.addStep("Level 1 Step", context -> {
                    executionOrder.add("Level 1 Step");
                    return PipelineResult.success("L1 Step done");
                });
                stage1.addStage("Level 2 Stage", stage2 -> {
                    stage2.addStep("Level 2 Step", context -> {
                        executionOrder.add("Level 2 Step");
                        return PipelineResult.success("L2 Step done");
                    });
                    stage2.addStage("Level 3 Stage", stage3 -> {
                        stage3.addStep("Level 3 Step", context -> {
                            executionOrder.add("Level 3 Step");
                            return PipelineResult.success("L3 Step done");
                        });
                    });
                });
            })
            .addStep("Final Step", context -> {
                executionOrder.add("Final Step");
                return PipelineResult.success("Final Step done");
            });

        PipelineResult result = pipeline.execute();

        assertTrue(result.isSuccess());
        assertEquals(List.of(
            "Step 1",
            "Level 1 Step",
            "Level 2 Step",
            "Level 3 Step",
            "Final Step"
        ), executionOrder);
    }

    @Test
    public void testListenerWithNesting() {
        List<String> events = new ArrayList<>();
        PipelineListener listener = new PipelineListener() {
            @Override
            public void onPipelineStart(Pipeline pipeline, PipelineContext context) {
                events.add("Pipeline start: " + pipeline.getName());
            }

            @Override
            public void onPipelineEnd(Pipeline pipeline, PipelineContext context, PipelineResult result) {
                events.add("Pipeline end: " + pipeline.getName());
            }

            @Override
            public void onStepStart(PipelineElement step, PipelineContext context) {
                events.add("Step start: " + step.getName());
            }

            @Override
            public void onStepEnd(PipelineElement step, PipelineContext context, PipelineResult result) {
                events.add("Step end: " + step.getName());
            }

            @Override
            public void onWarning(PipelineElement element, PipelineContext context, String message) {}

            @Override
            public void onError(PipelineElement element, PipelineContext context, Exception e) {}
        };

        Pipeline pipeline = Pipeline.create("TestPipeline")
            .addListener(listener)
            .addStage("Stage 1", stage -> {
                stage.addStep("Step 1.1", context -> PipelineResult.success("OK"));
            });

        pipeline.execute();

        // Überprüfung der Event-Reihenfolge
        // Pipeline Start
        //   Step Start: Stage 1
        //     Step Start: Step 1.1
        //     Step End: Step 1.1
        //   Step End: Stage 1
        // Pipeline End
        
        assertTrue(events.contains("Pipeline start: TestPipeline"));
        assertTrue(events.contains("Step start: Stage 1"));
        assertTrue(events.contains("Step start: Step 1.1"));
        assertTrue(events.contains("Step end: Step 1.1"));
        assertTrue(events.contains("Step end: Stage 1"));
        assertTrue(events.contains("Pipeline end: TestPipeline"));
        
        // Reihenfolge prüfen
        assertEquals("Pipeline start: TestPipeline", events.get(0));
        assertEquals("Step start: Stage 1", events.get(1));
        assertEquals("Step start: Step 1.1", events.get(2));
        assertEquals("Step end: Step 1.1", events.get(3));
        assertEquals("Step end: Stage 1", events.get(4));
        assertEquals("Pipeline end: TestPipeline", events.get(5));
    }
}
