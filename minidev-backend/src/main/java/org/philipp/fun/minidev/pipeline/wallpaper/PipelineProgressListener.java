package org.philipp.fun.minidev.pipeline.wallpaper;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.philipp.fun.minidev.pipeline.composite.CircuitBreaker;
import org.philipp.fun.minidev.pipeline.composite.Conditional;
import org.philipp.fun.minidev.pipeline.composite.ForkJoin;
import org.philipp.fun.minidev.pipeline.composite.Parallel;
import org.philipp.fun.minidev.pipeline.composite.Sequence;
import org.philipp.fun.minidev.pipeline.composite.Switch;
import org.philipp.fun.minidev.pipeline.composite.Timeout;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;
import org.philipp.fun.minidev.pipeline.core.PipelineListener;

/**
 * Listener that tracks pipeline execution progress and reports events via SSE.
 */
public class PipelineProgressListener implements PipelineListener {

    /** Node type constant for stage composites. */
    private static final String NODE_TYPE_STAGE = "STAGE";

    /** Node type constant for leaf steps. */
    private static final String NODE_TYPE_STEP = "STEP";

    /** Node type constant for conditional composites. */
    private static final String NODE_TYPE_CONDITIONAL = "CONDITIONAL";

    /** Node type constant for parallel composites. */
    private static final String NODE_TYPE_PARALLEL = "PARALLEL";

    /** Node type constant for timeout composites. */
    private static final String NODE_TYPE_TIMEOUT = "TIMEOUT";

    /** Node type constant for circuit breaker composites. */
    private static final String NODE_TYPE_CIRCUIT_BREAKER = "CIRCUIT_BREAKER";

    /** The run identifier. */
    private final String runId;

    /** The pipeline name. */
    private final String pipelineName;

    /** The root pipeline element. */
    private final PipelineElement rootElement;

    /** The SSE service for broadcasting progress. */
    private final PipelineProgressSseService progressSseService;

    /** Counter for generating unique node IDs. */
    private final AtomicInteger nodeCounter = new AtomicInteger(0);

    /** Map from pipeline elements to their node metadata. */
    private final Map<PipelineElement, NodeMeta> nodes = new IdentityHashMap<>();

    /** Per-thread execution stack for tracking parent-child relationships. */
    private final ThreadLocal<Deque<String>> executionStack =
            ThreadLocal.withInitial(ArrayDeque::new);

    /**
     * Constructs a new PipelineProgressListener.
     *
     * @param runId              the run identifier
     * @param pipelineName       the pipeline name
     * @param rootElement        the root pipeline element
     * @param progressSseService the SSE service for progress events
     */
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

    /**
     * Marks the root element as started.
     */
    public void markRootStarted() {
        NodeMeta root = getOrRegisterNode(rootElement, null);
        progressSseService.nodeStarted(
                runId, pipelineName, root.nodeId(), root.parentNodeId(),
                root.name(), root.type());
    }

    /**
     * Marks the root element as finished.
     *
     * @param success whether the execution succeeded
     */
    public void markRootFinished(boolean success) {
        NodeMeta root = getOrRegisterNode(rootElement, null);
        progressSseService.nodeFinished(
                runId, pipelineName, root.nodeId(), root.parentNodeId(),
                root.name(), root.type(), success);
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

        progressSseService.nodeStarted(
                runId, pipelineName, node.nodeId(), node.parentNodeId(),
                node.name(), node.type());
    }

    @Override
    public void onEnd(PipelineElement step, PipelineContext context, boolean result) {
        NodeMeta node = getOrRegisterNode(step, null);
        progressSseService.nodeFinished(
                runId, pipelineName, node.nodeId(), node.parentNodeId(),
                node.name(), node.type(), result);

        Deque<String> stack = executionStack.get();
        stack.removeLastOccurrence(node.nodeId());
    }

    @Override
    public void onWarning(PipelineElement element, PipelineContext context, String message) {
        NodeMeta node = getOrRegisterNode(element, null);
        progressSseService.nodeWarning(
                runId, pipelineName, node.nodeId(), node.parentNodeId(),
                node.name(), node.type(), message);
    }

    @Override
    public void onError(PipelineElement element, PipelineContext context, Exception e) {
        NodeMeta node = getOrRegisterNode(element, null);
        progressSseService.nodeError(
                runId, pipelineName, node.nodeId(), node.parentNodeId(),
                node.name(), node.type(), e);
    }

    /**
     * Recursively discovers and registers all nodes in the pipeline tree.
     *
     * @param element      the current element
     * @param parentNodeId the parent node ID
     */
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

    /**
     * Gets or registers a node for the given element.
     *
     * @param element      the pipeline element
     * @param parentNodeId the parent node ID
     * @return the node metadata
     */
    private synchronized NodeMeta getOrRegisterNode(
            PipelineElement element, String parentNodeId) {
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
        NodeMeta created = new NodeMeta(
                nodeId, parentNodeId, element.getName(), nodeType);
        nodes.put(element, created);

        progressSseService.nodeDiscovered(
                runId, pipelineName, created.nodeId(), created.parentNodeId(),
                created.name(), created.type());
        return created;
    }

    /**
     * Metadata for a pipeline node.
     *
     * @param nodeId       the unique node identifier
     * @param parentNodeId the parent node identifier
     * @param name         the node display name
     * @param type         the node type
     */
    private record NodeMeta(
            /** The unique node identifier. */
            String nodeId,
            /** The parent node identifier. */
            String parentNodeId,
            /** The node display name. */
            String name,
            /** The node type. */
            String type
    ) {
    }
}