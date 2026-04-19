package org.philipp.fun.minidev.pipeline.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LambdaStep and AbstractStep Tests")
class LambdaStepTest {

    @Nested
    @DisplayName("Execution")
    class ExecutionTests {

        @Test
        @DisplayName("Execute lambda function")
        void testLambdaExecution() throws Exception {
            // Arrange
            LambdaStep step = new LambdaStep("Test Step", context -> true);
            PipelineContext context = new PipelineContext();

            // Act
            boolean success = step.execute(context);

            // Assert
            assertThat(success).isTrue();
        }

        @Test
        @DisplayName("Validation fails for null context")
        void testNullContext() throws Exception {
            // Arrange
            LambdaStep step = new LambdaStep("Test Step", context -> true);

            // Act
            boolean success = step.execute(null);

            // Assert
            assertThat(success).isFalse();
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
            LambdaStep step = new LambdaStep(name, context -> true);

            // Act & Assert
            assertThat(step.getName()).isEqualTo(name);
        }
    }
}
