package org.philipp.fun.minidev.services;

import org.philipp.fun.minidev.dto.PipelineProgressEvent;
import org.philipp.fun.minidev.dto.PipelineProgressEventType;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class PipelineProgressSseService extends AbstractSseService {

    @Override
    public String getStreamId() {
        return "PIPELINE";
    }

    @Override
    protected boolean isHistoryEnabled() {
        return true;
    }

    public void startRun(String runId, String pipelineName) {
        sendClearCommand();
        sendEvent(runId, pipelineName, PipelineProgressEventType.RUN_STARTED, null, null, null, null, "RUNNING", null);
    }

    public void finishRun(String runId, String pipelineName, boolean success) {
        sendEvent(runId, pipelineName, PipelineProgressEventType.RUN_FINISHED, null, null, null, null, success ? "SUCCESS" : "FAILED", null);
    }

    public void nodeDiscovered(String runId, String pipelineName, String nodeId, String parentNodeId, String nodeName, String nodeType) {
        sendEvent(runId, pipelineName, PipelineProgressEventType.NODE_DISCOVERED, nodeId, parentNodeId, nodeName, nodeType, "PENDING", null);
    }

    public void nodeStarted(String runId, String pipelineName, String nodeId, String parentNodeId, String nodeName, String nodeType) {
        sendEvent(runId, pipelineName, PipelineProgressEventType.NODE_STARTED, nodeId, parentNodeId, nodeName, nodeType, "RUNNING", null);
    }

    public void nodeFinished(String runId, String pipelineName, String nodeId, String parentNodeId, String nodeName, String nodeType, boolean success) {
        sendEvent(runId, pipelineName, PipelineProgressEventType.NODE_FINISHED, nodeId, parentNodeId, nodeName, nodeType, success ? "SUCCESS" : "FAILED", null);
    }

    public void nodeWarning(String runId, String pipelineName, String nodeId, String parentNodeId, String nodeName, String nodeType, String message) {
        sendEvent(runId, pipelineName, PipelineProgressEventType.NODE_WARNING, nodeId, parentNodeId, nodeName, nodeType, "WARNING", message);
    }

    public void nodeError(String runId, String pipelineName, String nodeId, String parentNodeId, String nodeName, String nodeType, Exception error) {
        sendEvent(runId, pipelineName, PipelineProgressEventType.NODE_ERROR, nodeId, parentNodeId, nodeName, nodeType, "FAILED", error != null ? error.getMessage() : null);
    }

    private void sendEvent(
            String runId,
            String pipelineName,
            PipelineProgressEventType type,
            String nodeId,
            String parentNodeId,
            String nodeName,
            String nodeType,
            String status,
            String message
    ) {
        broadcast(SseEventName.EVENT, new PipelineProgressEvent(
                runId,
                pipelineName,
                type,
                nodeId,
                parentNodeId,
                nodeName,
                nodeType,
                status,
                message,
                Instant.now().toString()
        ));
    }
}

