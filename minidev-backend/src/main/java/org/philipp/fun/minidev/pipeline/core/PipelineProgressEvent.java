package org.philipp.fun.minidev.pipeline.core;

/**
 * Data record representing a progress event emitted during pipeline execution.
 *
 * @param runId        the unique run identifier
 * @param pipelineName the name of the pipeline
 * @param type         the event type
 * @param nodeId       the identifier of the node that emitted the event
 * @param parentNodeId the identifier of the parent node
 * @param nodeName     the display name of the node
 * @param nodeType     the type of the node
 * @param status       the current status
 * @param message      an optional human-readable message
 * @param timestamp    the ISO-8601 timestamp of the event
 */
public record PipelineProgressEvent(
        String runId,
        String pipelineName,
        PipelineProgressEventType type,
        String nodeId,
        String parentNodeId,
        String nodeName,
        String nodeType,
        String status,
        String message,
        String timestamp
) {
}