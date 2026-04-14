package org.philipp.fun.minidev.pipeline.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Pipeline Results Tests")
class PipelineResultTest {

    @Nested
    @DisplayName("StepResult Tests")
    class StepResultTests {
        @ParameterizedTest
        @EnumSource(StepResult.StepStatus.class)
        @DisplayName("isSuccess returns true only for SUCCESS status")
        void testIsSuccess(StepResult.StepStatus status) {
            // Arrange
            StepResult result = new StepResult(status, "test");

            // Act
            boolean isSuccess = result.isSuccess();

            // Assert
            if (status == StepResult.StepStatus.SUCCESS) {
                assertThat(isSuccess).isTrue();
            } else {
                assertThat(isSuccess).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("StageResult Tests")
    class StageResultTests {
        @ParameterizedTest
        @EnumSource(StageResult.StageStatus.class)
        @DisplayName("isSuccess returns true only for SUCCESS status")
        void testIsSuccess(StageResult.StageStatus status) {
            // Arrange
            StageResult result = new StageResult("stage", status, "test", null);

            // Act
            boolean isSuccess = result.isSuccess();

            // Assert
            if (status == StageResult.StageStatus.SUCCESS) {
                assertThat(isSuccess).isTrue();
            } else {
                assertThat(isSuccess).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("PipelineResult Tests")
    class PipelineResultTests {
        @ParameterizedTest
        @EnumSource(PipelineResult.PipelineStatus.class)
        @DisplayName("isSuccess returns true only for SUCCESS status")
        void testIsSuccess(PipelineResult.PipelineStatus status) {
            // Arrange
            PipelineResult result = new PipelineResult("pipeline", status, "test", null);

            // Act
            boolean isSuccess = result.isSuccess();

            // Assert
            if (status == PipelineResult.PipelineStatus.SUCCESS) {
                assertThat(isSuccess).isTrue();
            } else {
                assertThat(isSuccess).isFalse();
            }
        }
    }
}
