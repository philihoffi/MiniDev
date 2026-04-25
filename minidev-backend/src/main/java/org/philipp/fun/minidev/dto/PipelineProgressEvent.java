package org.philipp.fun.minidev.dto;

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

