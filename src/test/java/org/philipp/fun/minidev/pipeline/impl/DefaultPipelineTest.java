package org.philipp.fun.minidev.pipeline.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;
import org.philipp.fun.minidev.pipeline.core.PipelineListener;
import org.philipp.fun.minidev.pipeline.core.Step;
import org.philipp.fun.minidev.pipeline.model.PipelineResult;

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
            PipelineElement stage1 = mock(PipelineElement.class);
            PipelineElement stage2 = mock(PipelineElement.class);
            when(stage1.execute(context)).thenReturn(new PipelineResult("S1", PipelineResult.Status.SUCCESS, "OK", null));
            when(stage1.getName()).thenReturn("S1");
            when(stage2.execute(context)).thenReturn(new PipelineResult("S2", PipelineResult.Status.SUCCESS, "OK", null));
            when(stage2.getName()).thenReturn("S2");
            
            pipeline.addElement(stage1).addElement(stage2);

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
            PipelineElement stage1 = mock(PipelineElement.class);
            PipelineElement stage2 = mock(PipelineElement.class);
            PipelineResult failure = new PipelineResult("S1", PipelineResult.Status.FAILED, "Error", null);
            
            when(stage1.execute(context)).thenReturn(failure);
            when(stage1.getName()).thenReturn("S1");
            pipeline.addElement(stage1).addElement(stage2);

            // Act
            PipelineResult result = pipeline.execute(context);

            // Assert
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.cause()).isEqualTo(failure);
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
            PipelineElement stage1 = mock(PipelineElement.class);
            PipelineElement dynamicStage = mock(PipelineElement.class);
            
            when(stage1.execute(context)).thenAnswer(inv -> {
                context.getPipeline().addElement(dynamicStage);
                return new PipelineResult("S1", PipelineResult.Status.SUCCESS, "OK", null);
            });
            when(stage1.getName()).thenReturn("S1");
            when(dynamicStage.execute(context)).thenReturn(new PipelineResult("DS", PipelineResult.Status.SUCCESS, "OK", null));
            when(dynamicStage.getName()).thenReturn("DS");

            pipeline.addElement(stage1);

            // Act
            PipelineResult result = pipeline.execute(context);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            verify(stage1).execute(context);
            verify(dynamicStage).execute(context);
            assertThat(pipeline.getElements()).hasSize(2);
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
            
            PipelineElement stage = mock(PipelineElement.class);
            PipelineResult stageResult = new PipelineResult("S1", PipelineResult.Status.SUCCESS, "OK", null);
            when(stage.execute(context)).thenReturn(stageResult);
            when(stage.getName()).thenReturn("S1");
            pipeline.addElement(stage);

            // Act
            PipelineResult pipelineResult = pipeline.execute(context);

            // Assert
            verify(listener).onPipelineStart(pipeline, context);
            verify(listener).onStepStart(stage, context);
            verify(listener).onStepEnd(stage, context, stageResult);
            verify(listener).onPipelineEnd(pipeline, context, pipelineResult);
        }
    }
}
