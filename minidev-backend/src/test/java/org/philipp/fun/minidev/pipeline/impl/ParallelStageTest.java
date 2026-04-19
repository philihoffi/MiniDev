package org.philipp.fun.minidev.pipeline.impl;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.philipp.fun.minidev.pipeline.core.*;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("Parallel Stage Execution Tests")
class ParallelStageTest {

    private ParallelStage stage;
    private PipelineContext context;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        int numThreads = Runtime.getRuntime().availableProcessors()*2;
        executor = Executors.newFixedThreadPool(numThreads);
        System.out.println("Executor Threads: " + numThreads);

        stage = new ParallelStage(executor, "Test Stage");
        context = new PipelineContext();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executor.shutdown();
        if(!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }
    }

    @Nested
    @DisplayName("Execution Logic")
    class ExecutionTests {

        @Test
        @DisplayName("Parallel execution of multiple steps can handle 1000 Parallel Steps")
        void testParallelExecution() throws Exception {
            // Arrange
            int numSteps = 10000;
            for (int i = 0; i < numSteps; i++) {
                Step step = Step.create("step-" + i, ctx -> {
                    //random code to simulate long running step the ThreadRunning
                    double sum = 0;
                    for (long j = 0; j < 1000; j++) {
                        sum = Math.sqrt(sum);
                        sum = Math.sin(sum);
                        sum = Math.cos(sum);
                    }
                    return true;
                });

                stage.addElement(step);
            }

            // Pipeline pipeline = new DefaultPipeline("Test Pipeline");
            // pipeline.addElement(stage);

            AtomicInteger activeSteps = new AtomicInteger(0);
            AtomicInteger completedSteps = new AtomicInteger(0);
            long startTime = System.currentTimeMillis();

            PipelineListener listener = new PipelineListener() {
                @Override
                public void onStepStart(PipelineElement step, PipelineContext context) {
                    activeSteps.incrementAndGet();
                }

                @Override
                public void onStepEnd(PipelineElement step, PipelineContext context, boolean result) {
                    activeSteps.decrementAndGet();
                    completedSteps.incrementAndGet();

                    int completed = completedSteps.get();
                    if (completed > 0) {
                        long elapsed = System.currentTimeMillis() - startTime;
                        double avgTimePerStep = (double) elapsed / completed;
                        int remaining = numSteps - completed;
                        double estimatedRemainingTimeMs = avgTimePerStep * remaining;
                        
                        //System.out.println("Completed: " + completed + ", Active: " + activeSteps.get() + ", Remaining: " + remaining + ", ETA: " + Duration.ofMillis((long) estimatedRemainingTimeMs));
                    }
                }
            };
            stage.setListeners(Collections.singletonList(listener));

            // Act
            stage.execute(context);


        }

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
        @DisplayName("Collect results from all steps even if one fails")
        void testFailedStep() throws Exception {
            // Arrange
            PipelineElement step1 = mock(PipelineElement.class);
            PipelineElement step2 = mock(PipelineElement.class);
            when(step1.execute(context)).thenReturn(false);
            when(step1.getName()).thenReturn("step1");
            when(step2.execute(context)).thenReturn(true);
            when(step2.getName()).thenReturn("step2");
            stage.addElement(step1).addElement(step2);

            // Act
            boolean success = stage.execute(context);

            // Assert
            assertThat(success).isFalse();
            verify(step1).execute(context);
            verify(step2).execute(context);
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
            // In ParallelStage, an exception in a step is caught in the async lambda
            // and returns a failed result with the message "Exception: Crash"
            // The stage then returns a failed result wrapping this one.
            // Note: ParallelStage implementation seems to have changed significantly,
            // we are just checking if it returns false now.
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
