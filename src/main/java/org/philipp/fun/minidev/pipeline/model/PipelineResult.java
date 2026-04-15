package org.philipp.fun.minidev.pipeline.model;

public record PipelineResult(String name, Status status, String message, PipelineResult cause) {

    public static PipelineResult success(String name, String message) {
        return new PipelineResult(name, Status.SUCCESS, message, null);
    }

    public static PipelineResult success(String message) {
        return new PipelineResult("Unknown", Status.SUCCESS, message, null);
    }

    public static PipelineResult failed(String name, String message) {
        return new PipelineResult(name, Status.FAILED, message, null);
    }

    public static PipelineResult failed(String name, String message, PipelineResult cause) {
        return new PipelineResult(name, Status.FAILED, message, cause);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public enum Status {
        SUCCESS,
        FAILED,
        SKIPPED
    }
}
