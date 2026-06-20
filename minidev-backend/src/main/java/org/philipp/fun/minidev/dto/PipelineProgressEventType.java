package org.philipp.fun.minidev.dto;

/**
 * Enumeration of event types that can be emitted during pipeline execution.
 */
public enum PipelineProgressEventType {

    /** A pipeline run has started. */
    RUN_STARTED,
    /** A pipeline run has finished. */
    RUN_FINISHED,
    /** A new pipeline node has been discovered. */
    NODE_DISCOVERED,
    /** A pipeline node has started execution. */
    NODE_STARTED,
    /** A pipeline node has finished execution. */
    NODE_FINISHED,
    /** A pipeline node has emitted a warning. */
    NODE_WARNING,
    /** A pipeline node has encountered an error. */
    NODE_ERROR
}