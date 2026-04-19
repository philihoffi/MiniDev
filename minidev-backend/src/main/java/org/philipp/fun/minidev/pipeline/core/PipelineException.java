package org.philipp.fun.minidev.pipeline.core;

public class PipelineException extends Exception {
    public PipelineException(String message) {
        super(message);
    }

    public PipelineException(String message, Throwable cause) {
        super(message, cause);
    }
}
