package org.philipp.fun.minidev.pipeline.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;
import org.philipp.fun.minidev.pipeline.core.PipelineListener;
import org.philipp.fun.minidev.pipeline.core.Step;
import org.philipp.fun.minidev.pipeline.model.PipelineResult;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("Default Stage Execution Tests")
class SequenzStageTest {

    private SequenzStage stage;
    private PipelineContext context;

    @BeforeEach
    void setUp() {
        stage = new SequenzStage("Test Stage");
        context = new PipelineContext();
    }

    @Nested
    @DisplayName("Execution Logic")
    class ExecutionTests {

        @Test
        @DisplayName("Execute successful step")
        void testSuccessfulStep() {
            // Arrange
            PipelineElement step = mock(PipelineElement.class);
            PipelineResult successResult = new PipelineResult("step", PipelineResult.Status.SUCCESS, "OK", null);
            when(step.execute(context)).thenReturn(successResult);
            when(step.getName()).thenReturn("step");
            stage.addElement(step);

            // Act
            PipelineResult result = stage.execute(context);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.name()).isEqualTo(stage.getName());
            verify(step).execute(context);
        }

        @Test
        @DisplayName("Stop execution after failed step")
        void testFailedStep() {
            // Arrange
            PipelineElement step1 = mock(PipelineElement.class);
            PipelineElement step2 = mock(PipelineElement.class);
            PipelineResult failureResult = new PipelineResult("step1", PipelineResult.Status.FAILED, "Error", null);
            
            when(step1.execute(context)).thenReturn(failureResult);
            when(step1.getName()).thenReturn("step1");
            stage.addElement(step1).addElement(step2);

            // Act
            PipelineResult result = stage.execute(context);

            // Assert
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.cause()).isEqualTo(failureResult);
            verify(step1).execute(context);
            verify(step2, never()).execute(context);
        }

        @Test
        @DisplayName("Handle exception in step as failure")
        void testStepException() {
            // Arrange
            PipelineElement step = mock(PipelineElement.class);
            when(step.execute(context)).thenThrow(new RuntimeException("Crash"));
            when(step.getName()).thenReturn("step");
            stage.addElement(step);

            // Act
            PipelineResult result = stage.execute(context);

            // Assert
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.status()).isEqualTo(PipelineResult.Status.FAILED);
            assertThat(result.message()).contains("Crash");
        }
    }

    @Nested
    @DisplayName("Listener Notifications")
    class ListenerTests {

        @Test
        @DisplayName("Notify listener on step events")
        void testListenerNotification() {
            // Arrange
            PipelineListener listener = mock(PipelineListener.class);
            stage.setListeners(Collections.singletonList(listener));
            
            PipelineElement step = mock(PipelineElement.class);
            PipelineResult successResult = new PipelineResult("step", PipelineResult.Status.SUCCESS, "OK", null);
            when(step.execute(context)).thenReturn(successResult);
            when(step.getName()).thenReturn("step");
            stage.addElement(step);

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
            PipelineElement step = mock(PipelineElement.class);
            when(step.execute(context)).thenThrow(exception);
            when(step.getName()).thenReturn("step");
            stage.addElement(step);

            // Act
            stage.execute(context);

            // Assert
            verify(listener).onError(step, context, exception);
        }

        @Test
        @DisplayName("Propagate listeners to steps")
        void testStepListenerPropagation() throws Exception {
            // Arrange
            PipelineListener listener = mock(PipelineListener.class);
            stage.setListeners(Collections.singletonList(listener));

            PipelineElement step = mock(PipelineElement.class);
            when(step.execute(context)).thenReturn(new PipelineResult("step", PipelineResult.Status.SUCCESS, "OK", null));
            when(step.getName()).thenReturn("step");
            stage.addElement(step);

            // Act
            stage.execute(context);

            // Assert
            verify(step).setListeners(Collections.singletonList(listener));
        }
    }
    
    @Nested
    @DisplayName("Nested Stages")
    class NestedStageTests {

        @Test
        @DisplayName("Execute nested successful stages")
        void testNestedSuccessfulStages() {
            // Arrange
            SequenzStage innerStage = new SequenzStage("Inner Stage");
            PipelineElement step = mock(PipelineElement.class);
            PipelineResult successResult = new PipelineResult("step", PipelineResult.Status.SUCCESS, "OK", null);
            when(step.execute(context)).thenReturn(successResult);
            when(step.getName()).thenReturn("step");

            innerStage.addElement(step);
            stage.addElement(innerStage);

            // Act
            PipelineResult result = stage.execute(context);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            verify(step).execute(context);
        }

        @Test
        @DisplayName("Fail outer stage if nested stage fails")
        void testNestedFailedStage() {
            // Arrange
            SequenzStage innerStage = new SequenzStage("Inner Stage");
            PipelineElement step = mock(PipelineElement.class);
            PipelineResult failureResult = new PipelineResult("step", PipelineResult.Status.FAILED, "Error", null);
            when(step.execute(context)).thenReturn(failureResult);
            when(step.getName()).thenReturn("step");

            innerStage.addElement(step);
            stage.addElement(innerStage);

            // Act
            PipelineResult result = stage.execute(context);

            // Assert
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.cause()).isNotNull();
            assertThat(result.cause().name()).isEqualTo("Inner Stage");
            assertThat(result.cause().cause()).isEqualTo(failureResult);
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
        void testStepStatusEffect(PipelineResult.Status stepStatus, boolean expectedStageSuccess) {
            // Arrange
            PipelineElement step = mock(PipelineElement.class);
            when(step.execute(context)).thenReturn(new PipelineResult("step", stepStatus, "Msg", null));
            when(step.getName()).thenReturn("step");
            stage.addElement(step);

            // Act
            PipelineResult result = stage.execute(context);

            // Assert
            assertThat(result.isSuccess()).isEqualTo(expectedStageSuccess);
        }
    }
}
