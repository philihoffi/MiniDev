package org.philipp.fun.minidev.pipeline.model;

import org.philipp.fun.minidev.pipeline.core.PipelineContext;

public record PipelineResult(String name, Status status, String message, PipelineResult cause, PipelineContext context) {

    public PipelineResult(String name, Status status, String message, PipelineResult cause) {
        this(name, status, message, cause, null);
    }

    public static PipelineResult success(String name, String message) {
        return new PipelineResult(name, Status.SUCCESS, message, null, null);
    }

    public static PipelineResult success(String message) {
        return new PipelineResult("Unknown", Status.SUCCESS, message, null, null);
    }

    public static PipelineResult failed(String name, String message) {
        return new PipelineResult(name, Status.FAILED, message, null, null);
    }

    public static PipelineResult failed(String name, String message, PipelineResult cause) {
        return new PipelineResult(name, Status.FAILED, message, cause, null);
    }

    public static PipelineResult success(String name, String message, PipelineContext context) {
        return new PipelineResult(name, Status.SUCCESS, message, null, context);
    }

    public static PipelineResult success(String message, PipelineContext context) {
        return new PipelineResult("Unknown", Status.SUCCESS, message, null, context);
    }

    public static PipelineResult failed(String name, String message, PipelineContext context) {
        return new PipelineResult(name, Status.FAILED, message, null, context);
    }

    public static PipelineResult failed(String name, String message, PipelineResult cause, PipelineContext context) {
        return new PipelineResult(name, Status.FAILED, message, cause, context);
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
