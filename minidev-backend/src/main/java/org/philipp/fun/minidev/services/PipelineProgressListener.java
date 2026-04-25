package org.philipp.fun.minidev.services;

import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;
import org.philipp.fun.minidev.pipeline.core.PipelineListener;
import org.philipp.fun.minidev.pipeline.core.Stage;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class PipelineProgressListener implements PipelineListener {

    private static final String NODE_TYPE_STAGE = "STAGE";
    private static final String NODE_TYPE_STEP = "STEP";

    private final String runId;
    private final String pipelineName;
    private final PipelineElement rootElement;
    private final PipelineProgressSseService progressSseService;
    private final AtomicInteger nodeCounter = new AtomicInteger(0);
    private final Map<PipelineElement, NodeMeta> nodes = new IdentityHashMap<>();
    private final ThreadLocal<Deque<String>> executionStack = ThreadLocal.withInitial(ArrayDeque::new);

    public PipelineProgressListener(
            String runId,
            String pipelineName,
            PipelineElement rootElement,
            PipelineProgressSseService progressSseService
    ) {
        this.runId = runId;
        this.pipelineName = pipelineName;
        this.rootElement = rootElement;
        this.progressSseService = progressSseService;
        discoverTree(rootElement, null);
    }

    public void markRootStarted() {
        NodeMeta root = getOrRegisterNode(rootElement, null);
        progressSseService.nodeStarted(runId, pipelineName, root.nodeId(), root.parentNodeId(), root.name(), root.type());
    }

    public void markRootFinished(boolean success) {
        NodeMeta root = getOrRegisterNode(rootElement, null);
        progressSseService.nodeFinished(runId, pipelineName, root.nodeId(), root.parentNodeId(), root.name(), root.type(), success);
        executionStack.remove();
    }

    @Override
    public void onStepStart(PipelineElement step, PipelineContext context) {
        Deque<String> stack = executionStack.get();
        if (stack.isEmpty()) {
            stack.addLast(getOrRegisterNode(rootElement, null).nodeId());
        }

        String parentNodeId = stack.peekLast();
        NodeMeta node = getOrRegisterNode(step, parentNodeId);
        stack.addLast(node.nodeId());

        progressSseService.nodeStarted(runId, pipelineName, node.nodeId(), node.parentNodeId(), node.name(), node.type());
    }

    @Override
    public void onStepEnd(PipelineElement step, PipelineContext context, boolean result) {
        NodeMeta node = getOrRegisterNode(step, null);
        progressSseService.nodeFinished(runId, pipelineName, node.nodeId(), node.parentNodeId(), node.name(), node.type(), result);

        Deque<String> stack = executionStack.get();
        stack.removeLastOccurrence(node.nodeId());
    }

    @Override
    public void onWarning(PipelineElement element, PipelineContext context, String message) {
        NodeMeta node = getOrRegisterNode(element, null);
        progressSseService.nodeWarning(runId, pipelineName, node.nodeId(), node.parentNodeId(), node.name(), node.type(), message);
    }

    @Override
    public void onError(PipelineElement element, PipelineContext context, Exception e) {
        NodeMeta node = getOrRegisterNode(element, null);
        progressSseService.nodeError(runId, pipelineName, node.nodeId(), node.parentNodeId(), node.name(), node.type(), e);
    }

    private void discoverTree(PipelineElement element, String parentNodeId) {
        NodeMeta node = getOrRegisterNode(element, parentNodeId);
        if (element instanceof Stage stage) {
            for (PipelineElement child : stage.getElements()) {
                discoverTree(child, node.nodeId());
            }
        }
    }

    private synchronized NodeMeta getOrRegisterNode(PipelineElement element, String parentNodeId) {
        NodeMeta existing = nodes.get(element);
        if (existing != null) {
            return existing;
        }

        String nodeId = "node-" + nodeCounter.incrementAndGet();
        String nodeType = element instanceof Stage ? NODE_TYPE_STAGE : NODE_TYPE_STEP;
        NodeMeta created = new NodeMeta(nodeId, parentNodeId, element.getName(), nodeType);
        nodes.put(element, created);

        progressSseService.nodeDiscovered(
                runId,
                pipelineName,
                created.nodeId(),
                created.parentNodeId(),
                created.name(),
                created.type()
        );
        return created;
    }

    private record NodeMeta(String nodeId, String parentNodeId, String name, String type) {
    }
}

