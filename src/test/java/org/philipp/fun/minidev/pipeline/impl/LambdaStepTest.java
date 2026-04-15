package org.philipp.fun.minidev.pipeline.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.model.PipelineResult;
import org.philipp.fun.minidev.pipeline.impl.LambdaStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LambdaStep and AbstractStep Tests")
class LambdaStepTest {

    @Nested
    @DisplayName("Execution")
    class ExecutionTests {

        @Test
        @DisplayName("Execute lambda function")
        void testLambdaExecution() {
            // Arrange
            PipelineResult expected = new PipelineResult("Test Step", PipelineResult.Status.SUCCESS, "OK", null);
            LambdaStep step = new LambdaStep("Test Step", context -> expected);
            PipelineContext context = new PipelineContext();

            // Act
            PipelineResult result = step.execute(context);

            // Assert
            assertThat(result).isSameAs(expected);
        }

        @Test
        @DisplayName("Validation fails for null context")
        void testNullContext() {
            // Arrange
            LambdaStep step = new LambdaStep("Test Step", context -> null);

            // Act
            PipelineResult result = step.execute(null);

            // Assert
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.message()).contains("context must not be null");
        }
    }

    @Nested
    @DisplayName("Metadata")
    class MetadataTests {
        @Test
        @DisplayName("Get step name from abstract base")
        void testGetName() {
            // Arrange
            String name = "Custom Name";
            LambdaStep step = new LambdaStep(name, context -> null);

            // Act & Assert
            assertThat(step.getName()).isEqualTo(name);
        }
    }
}
