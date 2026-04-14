package org.philipp.fun.minidev.pipeline.model;

public record StepResult(StepStatus status, String message) {

    public boolean isSuccess() {
        return status == StepStatus.SUCCESS;
    }

    public enum StepStatus {
        SUCCESS,
        FAILED,
        SKIPPED
    }
}
