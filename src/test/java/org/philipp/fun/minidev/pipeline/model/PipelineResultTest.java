package org.philipp.fun.minidev.pipeline.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Pipeline Results Tests")
class PipelineResultTest {

    @ParameterizedTest
    @EnumSource(PipelineResult.Status.class)
    @DisplayName("isSuccess returns true only for SUCCESS status")
    void testIsSuccess(PipelineResult.Status status) {
        // Arrange
        PipelineResult result = new PipelineResult("test", status, "test", null);

        // Act
        boolean isSuccess = result.isSuccess();

        // Assert
        if (status == PipelineResult.Status.SUCCESS) {
            assertThat(isSuccess).isTrue();
        } else {
            assertThat(isSuccess).isFalse();
        }
    }
}
