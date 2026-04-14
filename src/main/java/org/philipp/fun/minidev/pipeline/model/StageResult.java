package org.philipp.fun.minidev.pipeline.model;

public record StageResult(String stageName, StageStatus status, String message, StepResult failedStepResult) {

    public boolean isSuccess() {
        return status == StageStatus.SUCCESS;
    }

    public enum StageStatus {
        SUCCESS,
        FAILED,
        SKIPPED
    }
}
