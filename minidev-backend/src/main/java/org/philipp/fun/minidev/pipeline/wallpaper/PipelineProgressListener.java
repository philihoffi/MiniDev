package org.philipp.fun.minidev.pipeline.wallpaper;

import org.philipp.fun.minidev.pipeline.composite.Conditional;
import org.philipp.fun.minidev.pipeline.composite.Sequence;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;
import org.philipp.fun.minidev.pipeline.core.PipelineListener;
import org.philipp.fun.minidev.pipeline.composite.CircuitBreaker;
import org.philipp.fun.minidev.pipeline.composite.ForkJoin;
import org.philipp.fun.minidev.pipeline.composite.Parallel;
import org.philipp.fun.minidev.pipeline.composite.Switch;
import org.philipp.fun.minidev.pipeline.composite.Timeout;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class PipelineProgressListener implements PipelineListener {

    private static final String NODE_TYPE_STAGE = "STAGE";
    private static final String NODE_TYPE_STEP = "STEP";
    private static final String NODE_TYPE_CONDITIONAL = "CONDITIONAL";
    private static final String NODE_TYPE_PARALLEL = "PARALLEL";
    private static final String NODE_TYPE_TIMEOUT = "TIMEOUT";
    private static final String NODE_TYPE_CIRCUIT_BREAKER = "CIRCUIT_BREAKER";

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
    public void onStart(PipelineElement step, PipelineContext context) {
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
    public void onEnd(PipelineElement step, PipelineContext context, boolean result) {
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
        if (element instanceof Sequence stage) {
            for (PipelineElement child : stage.getElements()) {
                discoverTree(child, node.nodeId());
            }
        } else if (element instanceof ForkJoin fj) {
            for (PipelineElement child : fj.getForks()) {
                discoverTree(child, node.nodeId());
            }
            discoverTree(fj.getJoin(), node.nodeId());
        } else if (element instanceof Parallel parallel) {
            for (PipelineElement child : parallel.getElements()) {
                discoverTree(child, node.nodeId());
            }
        } else if (element instanceof Switch sw) {
            for (var c : sw.getCases()) {
                discoverTree(c.branch(), node.nodeId());
            }
            if (sw.getDefaultBranch() != null) {
                discoverTree(sw.getDefaultBranch(), node.nodeId());
            }
        } else if (element instanceof Timeout timeout) {
            discoverTree(timeout.getElement(), node.nodeId());
        } else if (element instanceof CircuitBreaker cb) {
            discoverTree(cb.getElement(), node.nodeId());
        } else if (element instanceof Conditional conditional) {
            discoverTree(conditional.getThenBranch(), node.nodeId());
            if (conditional.getElseBranch() != null) {
                discoverTree(conditional.getElseBranch(), node.nodeId());
            }
        }
    }

    private synchronized NodeMeta getOrRegisterNode(PipelineElement element, String parentNodeId) {
        NodeMeta existing = nodes.get(element);
        if (existing != null) {
            return existing;
        }

        String nodeId = "node-" + nodeCounter.incrementAndGet();
        String nodeType;
        if (element instanceof Sequence) {
            nodeType = NODE_TYPE_STAGE;
        } else if (element instanceof Conditional || element instanceof Switch) {
            nodeType = NODE_TYPE_CONDITIONAL;
        } else if (element instanceof Parallel || element instanceof ForkJoin) {
            nodeType = NODE_TYPE_PARALLEL;
        } else if (element instanceof Timeout) {
            nodeType = NODE_TYPE_TIMEOUT;
        } else if (element instanceof CircuitBreaker) {
            nodeType = NODE_TYPE_CIRCUIT_BREAKER;
        } else {
            nodeType = NODE_TYPE_STEP;
        }
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

