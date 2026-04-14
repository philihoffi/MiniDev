package org.philipp.fun.minidev.pipeline.model;

public record PipelineResult(String pipelineName, PipelineStatus status, String message, StageResult failedStageResult) {

    public boolean isSuccess() {
        return status == PipelineStatus.SUCCESS;
    }

    public enum PipelineStatus {
        SUCCESS,
        FAILED,
        SKIPPED
    }
}
