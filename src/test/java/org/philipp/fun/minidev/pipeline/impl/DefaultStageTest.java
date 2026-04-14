package org.philipp.fun.minidev.pipeline.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineListener;
import org.philipp.fun.minidev.pipeline.core.Step;
import org.philipp.fun.minidev.pipeline.model.StageResult;
import org.philipp.fun.minidev.pipeline.model.StepResult;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("Default Stage Execution Tests")
class DefaultStageTest {

    private DefaultStage stage;
    private PipelineContext context;

    @BeforeEach
    void setUp() {
        stage = new DefaultStage("Test Stage");
        context = new PipelineContext();
    }

    @Nested
    @DisplayName("Execution Logic")
    class ExecutionTests {

        @Test
        @DisplayName("Execute successful step")
        void testSuccessfulStep() throws Exception {
            // Arrange
            Step step = mock(Step.class);
            StepResult successResult = new StepResult(StepResult.StepStatus.SUCCESS, "OK");
            when(step.execute(context)).thenReturn(successResult);
            stage.addStep(step);

            // Act
            StageResult result = stage.execute(context);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.stageName()).isEqualTo(stage.getName());
            verify(step).execute(context);
        }

        @Test
        @DisplayName("Stop execution after failed step")
        void testFailedStep() throws Exception {
            // Arrange
            Step step1 = mock(Step.class);
            Step step2 = mock(Step.class);
            StepResult failureResult = new StepResult(StepResult.StepStatus.FAILED, "Error");
            
            when(step1.execute(context)).thenReturn(failureResult);
            stage.addStep(step1).addStep(step2);

            // Act
            StageResult result = stage.execute(context);

            // Assert
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.failedStepResult()).isEqualTo(failureResult);
            verify(step1).execute(context);
            verify(step2, never()).execute(context);
        }

        @Test
        @DisplayName("Handle exception in step as failure")
        void testStepException() throws Exception {
            // Arrange
            Step step = mock(Step.class);
            when(step.execute(context)).thenThrow(new RuntimeException("Crash"));
            stage.addStep(step);

            // Act
            StageResult result = stage.execute(context);

            // Assert
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.status()).isEqualTo(StageResult.StageStatus.FAILED);
            assertThat(result.message()).contains("Crash");
        }
    }

    @Nested
    @DisplayName("Listener Notifications")
    class ListenerTests {

        @Test
        @DisplayName("Notify listener on step events")
        void testListenerNotification() throws Exception {
            // Arrange
            PipelineListener listener = mock(PipelineListener.class);
            stage.setListeners(Collections.singletonList(listener));
            
            Step step = mock(Step.class);
            StepResult successResult = new StepResult(StepResult.StepStatus.SUCCESS, "OK");
            when(step.execute(context)).thenReturn(successResult);
            stage.addStep(step);

            // Act
            stage.execute(context);

            // Assert
            verify(listener).onStepStart(step, context);
            verify(listener).onStepEnd(step, context, successResult);
        }

        @Test
        @DisplayName("Notify listener on step error")
        void testErrorNotification() throws Exception {
            // Arrange
            PipelineListener listener = mock(PipelineListener.class);
            stage.setListeners(Collections.singletonList(listener));
            
            RuntimeException exception = new RuntimeException("Error");
            Step step = mock(Step.class);
            when(step.execute(context)).thenThrow(exception);
            stage.addStep(step);

            // Act
            stage.execute(context);

            // Assert
            verify(listener).onError(step, context, exception);
        }
    }
    
    @Nested
    @DisplayName("Parameterized Step Tests")
    class ParameterizedTests {
        @ParameterizedTest
        @CsvSource({
            "SUCCESS, true",
            "FAILED, false"
        })
        @DisplayName("Stage result depends on step status")
        void testStepStatusEffect(StepResult.StepStatus stepStatus, boolean expectedStageSuccess) throws Exception {
            // Arrange
            Step step = mock(Step.class);
            when(step.execute(context)).thenReturn(new StepResult(stepStatus, "Msg"));
            stage.addStep(step);

            // Act
            StageResult result = stage.execute(context);

            // Assert
            assertThat(result.isSuccess()).isEqualTo(expectedStageSuccess);
        }
    }
}
