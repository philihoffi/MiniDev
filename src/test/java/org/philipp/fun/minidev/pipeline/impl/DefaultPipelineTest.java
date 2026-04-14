package org.philipp.fun.minidev.pipeline.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineListener;
import org.philipp.fun.minidev.pipeline.core.Stage;
import org.philipp.fun.minidev.pipeline.model.PipelineResult;
import org.philipp.fun.minidev.pipeline.model.StageResult;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("Default Pipeline Execution Tests")
class DefaultPipelineTest {

    private DefaultPipeline pipeline;
    private PipelineContext context;

    @BeforeEach
    void setUp() {
        pipeline = new DefaultPipeline("Test Pipeline");
        context = new PipelineContext();
    }

    @Nested
    @DisplayName("Basic Execution")
    class BasicExecutionTests {

        @Test
        @DisplayName("Execute empty pipeline")
        void testEmptyPipeline() {
            // Act
            PipelineResult result = pipeline.execute(context);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            assertThat(context.getPipeline()).isSameAs(pipeline);
        }

        @Test
        @DisplayName("Execute successful stages")
        void testSuccessfulStages() {
            // Arrange
            Stage stage1 = mock(Stage.class);
            Stage stage2 = mock(Stage.class);
            when(stage1.execute(context)).thenReturn(new StageResult("S1", StageResult.StageStatus.SUCCESS, "OK", null));
            when(stage2.execute(context)).thenReturn(new StageResult("S2", StageResult.StageStatus.SUCCESS, "OK", null));
            
            pipeline.addStage(stage1).addStage(stage2);

            // Act
            PipelineResult result = pipeline.execute(context);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            verify(stage1).execute(context);
            verify(stage2).execute(context);
        }

        @Test
        @DisplayName("Stop after failed stage")
        void testFailedStage() {
            // Arrange
            Stage stage1 = mock(Stage.class);
            Stage stage2 = mock(Stage.class);
            StageResult failure = new StageResult("S1", StageResult.StageStatus.FAILED, "Error", null);
            
            when(stage1.execute(context)).thenReturn(failure);
            pipeline.addStage(stage1).addStage(stage2);

            // Act
            PipelineResult result = pipeline.execute(context);

            // Assert
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.failedStageResult()).isEqualTo(failure);
            verify(stage1).execute(context);
            verify(stage2, never()).execute(context);
        }
    }

    @Nested
    @DisplayName("Dynamic Stages")
    class DynamicStageTests {

        @Test
        @DisplayName("Execute stage added during execution")
        void testDynamicStageAddition() {
            // Arrange
            Stage stage1 = mock(Stage.class);
            Stage dynamicStage = mock(Stage.class);
            
            when(stage1.execute(context)).thenAnswer(inv -> {
                context.getPipeline().addStage(dynamicStage);
                return new StageResult("S1", StageResult.StageStatus.SUCCESS, "OK", null);
            });
            when(dynamicStage.execute(context)).thenReturn(new StageResult("DS", StageResult.StageStatus.SUCCESS, "OK", null));

            pipeline.addStage(stage1);

            // Act
            PipelineResult result = pipeline.execute(context);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            verify(stage1).execute(context);
            verify(dynamicStage).execute(context);
            assertThat(pipeline.getStages()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Pipeline Listeners")
    class ListenerTests {

        @Test
        @DisplayName("Notify all pipeline events")
        void testPipelineEvents() {
            // Arrange
            PipelineListener listener = mock(PipelineListener.class);
            pipeline.addListener(listener);
            
            Stage stage = mock(Stage.class);
            StageResult stageResult = new StageResult("S1", StageResult.StageStatus.SUCCESS, "OK", null);
            when(stage.execute(context)).thenReturn(stageResult);
            pipeline.addStage(stage);

            // Act
            PipelineResult pipelineResult = pipeline.execute(context);

            // Assert
            verify(listener).onPipelineStart(pipeline, context);
            verify(listener).onStageStart(stage, context);
            verify(listener).onStageEnd(stage, context, stageResult);
            verify(listener).onPipelineEnd(pipeline, context, pipelineResult);
        }
    }
}
