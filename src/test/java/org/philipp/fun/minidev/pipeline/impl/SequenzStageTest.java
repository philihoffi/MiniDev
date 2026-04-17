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

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
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
        void testSuccessfulStep() throws Exception {
            // Arrange
            PipelineElement step = mock(PipelineElement.class);
            when(step.execute(context)).thenReturn(true);
            when(step.getName()).thenReturn("step");
            stage.addElement(step);

            // Act
            boolean success = stage.execute(context);

            // Assert
            assertThat(success).isTrue();
            verify(step).execute(context);
        }

        @Test
        @DisplayName("Stop execution after failed step")
        void testFailedStep() throws Exception {
            // Arrange
            PipelineElement step1 = mock(PipelineElement.class);
            PipelineElement step2 = mock(PipelineElement.class);
            
            when(step1.execute(context)).thenReturn(false);
            when(step1.getName()).thenReturn("step1");
            stage.addElement(step1).addElement(step2);

            // Act
            boolean success = stage.execute(context);

            // Assert
            assertThat(success).isFalse();
            verify(step1).execute(context);
            verify(step2, never()).execute(context);
        }

        @Test
        @DisplayName("Handle exception in step as failure")
        void testStepException() throws Exception {
            // Arrange
            PipelineElement step = mock(PipelineElement.class);
            when(step.execute(context)).thenThrow(new RuntimeException("Crash"));
            when(step.getName()).thenReturn("step");
            stage.addElement(step);

            // Act
            boolean success = stage.execute(context);

            // Assert
            assertThat(success).isFalse();
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
            
            PipelineElement step = mock(PipelineElement.class);
            when(step.execute(context)).thenReturn(true);
            when(step.getName()).thenReturn("step");
            stage.addElement(step);

            // Act
            stage.execute(context);

            // Assert
            verify(listener).onStepStart(step, context);
            verify(listener).onStepEnd(step, context, true);
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
            when(step.execute(context)).thenReturn(true);
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
        void testNestedSuccessfulStages() throws Exception {
            // Arrange
            SequenzStage innerStage = new SequenzStage("Inner Stage");
            PipelineElement step = mock(PipelineElement.class);
            when(step.execute(context)).thenReturn(true);
            when(step.getName()).thenReturn("step");

            innerStage.addElement(step);
            stage.addElement(innerStage);

            // Act
            boolean success = stage.execute(context);

            // Assert
            assertThat(success).isTrue();
            verify(step).execute(context);
        }

        @Test
        @DisplayName("Fail outer stage if nested stage fails")
        void testNestedFailedStage() throws Exception {
            // Arrange
            SequenzStage innerStage = new SequenzStage("Inner Stage");
            PipelineElement step = mock(PipelineElement.class);
            when(step.execute(context)).thenReturn(false);
            when(step.getName()).thenReturn("step");

            innerStage.addElement(step);
            stage.addElement(innerStage);

            // Act
            boolean success = stage.execute(context);

            // Assert
            assertThat(success).isFalse();
        }
    }

    @Nested
    @DisplayName("Parameterized Step Tests")
    class ParameterizedTests {
        @ParameterizedTest
        @CsvSource({
            "true, true",
            "false, false"
        })
        @DisplayName("Stage result depends on step status")
        void testStepStatusEffect(boolean stepStatus, boolean expectedStageSuccess) throws Exception {
            // Arrange
            PipelineElement step = mock(PipelineElement.class);
            when(step.execute(context)).thenReturn(stepStatus);
            when(step.getName()).thenReturn("step");
            stage.addElement(step);

            // Act
            boolean success = stage.execute(context);

            // Assert
            assertThat(success).isEqualTo(expectedStageSuccess);
        }
    }
}
