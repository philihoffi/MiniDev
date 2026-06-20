package org.philipp.fun.minidev.pipeline.wallpaper;

import java.time.Instant;

import org.philipp.fun.minidev.dto.PipelineProgressEvent;
import org.philipp.fun.minidev.dto.PipelineProgressEventType;
import org.philipp.fun.minidev.sse.AbstractSseService;
import org.philipp.fun.minidev.sse.SseEventName;
import org.springframework.stereotype.Service;

/**
 * SSE service for broadcasting pipeline progress events.
 */
@Service
public class PipelineProgressSseService extends AbstractSseService {

    /**
     * Returns the stream identifier.
     *
     * @return the stream ID
     */
    @Override
    public String getStreamId() {
        return "PIPELINE";
    }

    /**
     * Returns whether history is enabled.
     *
     * @return true if history is enabled
     */
    @Override
    protected boolean isHistoryEnabled() {
        return true;
    }

    /**
     * Sends a run-started event.
     *
     * @param runId        the run ID
     * @param pipelineName the pipeline name
     */
    public void startRun(String runId, String pipelineName) {
        sendClearCommand();
        sendEvent(runId, pipelineName, PipelineProgressEventType.RUN_STARTED,
                null, null, null, null, "RUNNING", null);
    }

    /**
     * Sends a run-finished event.
     *
     * @param runId        the run ID
     * @param pipelineName the pipeline name
     * @param success      whether the run succeeded
     */
    public void finishRun(String runId, String pipelineName, boolean success) {
        sendEvent(runId, pipelineName, PipelineProgressEventType.RUN_FINISHED,
                null, null, null, null, success ? "SUCCESS" : "FAILED", null);
    }

    /**
     * Sends a node-discovered event.
     *
     * @param runId        the run ID
     * @param pipelineName the pipeline name
     * @param nodeId       the node ID
     * @param parentNodeId the parent node ID
     * @param nodeName     the node name
     * @param nodeType     the node type
     */
    public void nodeDiscovered(String runId, String pipelineName, String nodeId,
            String parentNodeId, String nodeName, String nodeType) {
        sendEvent(runId, pipelineName, PipelineProgressEventType.NODE_DISCOVERED,
                nodeId, parentNodeId, nodeName, nodeType, "PENDING", null);
    }

    /**
     * Sends a node-started event.
     *
     * @param runId        the run ID
     * @param pipelineName the pipeline name
     * @param nodeId       the node ID
     * @param parentNodeId the parent node ID
     * @param nodeName     the node name
     * @param nodeType     the node type
     */
    public void nodeStarted(String runId, String pipelineName, String nodeId,
            String parentNodeId, String nodeName, String nodeType) {
        sendEvent(runId, pipelineName, PipelineProgressEventType.NODE_STARTED,
                nodeId, parentNodeId, nodeName, nodeType, "RUNNING", null);
    }

    /**
     * Sends a node-finished event.
     *
     * @param runId        the run ID
     * @param pipelineName the pipeline name
     * @param nodeId       the node ID
     * @param parentNodeId the parent node ID
     * @param nodeName     the node name
     * @param nodeType     the node type
     * @param success      whether the node succeeded
     */
    public void nodeFinished(String runId, String pipelineName, String nodeId,
            String parentNodeId, String nodeName, String nodeType, boolean success) {
        sendEvent(runId, pipelineName, PipelineProgressEventType.NODE_FINISHED,
                nodeId, parentNodeId, nodeName, nodeType,
                success ? "SUCCESS" : "FAILED", null);
    }

    /**
     * Sends a node-warning event.
     *
     * @param runId        the run ID
     * @param pipelineName the pipeline name
     * @param nodeId       the node ID
     * @param parentNodeId the parent node ID
     * @param nodeName     the node name
     * @param nodeType     the node type
     * @param message      the warning message
     */
    public void nodeWarning(String runId, String pipelineName, String nodeId,
            String parentNodeId, String nodeName, String nodeType, String message) {
        sendEvent(runId, pipelineName, PipelineProgressEventType.NODE_WARNING,
                nodeId, parentNodeId, nodeName, nodeType, "WARNING", message);
    }

    /**
     * Sends a node-error event.
     *
     * @param runId        the run ID
     * @param pipelineName the pipeline name
     * @param nodeId       the node ID
     * @param parentNodeId the parent node ID
     * @param nodeName     the node name
     * @param nodeType     the node type
     * @param error        the exception
     */
    public void nodeError(String runId, String pipelineName, String nodeId,
            String parentNodeId, String nodeName, String nodeType, Exception error) {
        sendEvent(runId, pipelineName, PipelineProgressEventType.NODE_ERROR,
                nodeId, parentNodeId, nodeName, nodeType, "FAILED",
                error != null ? error.getMessage() : null);
    }

    /**
     * Broadcasts a pipeline progress event.
     *
     * @param runId        the run ID
     * @param pipelineName the pipeline name
     * @param type         the event type
     * @param nodeId       the node ID
     * @param parentNodeId the parent node ID
     * @param nodeName     the node name
     * @param nodeType     the node type
     * @param status       the status string
     * @param message      the optional message
     */
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