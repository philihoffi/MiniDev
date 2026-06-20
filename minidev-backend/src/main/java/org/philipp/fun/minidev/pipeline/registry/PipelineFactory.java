package org.philipp.fun.minidev.pipeline.registry;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.philipp.fun.minidev.pipeline.composite.CircuitBreaker;
import org.philipp.fun.minidev.pipeline.composite.Conditional;
import org.philipp.fun.minidev.pipeline.composite.ForkJoin;
import org.philipp.fun.minidev.pipeline.composite.Parallel;
import org.philipp.fun.minidev.pipeline.composite.Retry;
import org.philipp.fun.minidev.pipeline.composite.Sequence;
import org.philipp.fun.minidev.pipeline.composite.Switch;
import org.philipp.fun.minidev.pipeline.composite.Timeout;
import org.philipp.fun.minidev.pipeline.config.PipelineDefinition;
import org.philipp.fun.minidev.pipeline.config.PipelineDefinition.StageDef;
import org.philipp.fun.minidev.pipeline.core.ContextKey;
import org.philipp.fun.minidev.pipeline.core.ContextKeys;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;
import org.philipp.fun.minidev.pipeline.core.PipelineListener;
import org.philipp.fun.minidev.pipeline.hook.HookManager;
import org.philipp.fun.minidev.pipeline.impl.GenericLlmStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Factory for building pipeline instances from {@link PipelineDefinition} objects.
 */
@Component
public class PipelineFactory {

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(PipelineFactory.class);

    /** Registry for available leaf stages. */
    private final StageRegistry stageRegistry;

    /** Manager for before/after/around hooks. */
    private final HookManager hookManager;

    /**
     * Constructs a new PipelineFactory.
     *
     * @param stageRegistry the stage registry
     * @param hookManager   the hook manager
     */
    public PipelineFactory(StageRegistry stageRegistry, HookManager hookManager) {
        this.stageRegistry = stageRegistry;
        this.hookManager = hookManager;
    }

    /**
     * Creates a pipeline from the given definition.
     *
     * @param def the pipeline definition
     * @return the root pipeline element
     */
    public PipelineElement createPipeline(PipelineDefinition def) {
        LOG.info("Building pipeline '{}' from definition", def.getName());
        if (def.getStages().size() == 1) {
            StageDef top = def.getStages().getFirst();
            PipelineElement root = buildElement(top, def.getName());
            return hookManager.wrapWithHooks(root, def.getName());
        }
        Sequence pipeline = new Sequence(def.getName());
        for (StageDef stageDef : def.getStages()) {
            PipelineElement element = buildElement(stageDef, def.getName());
            pipeline.add(element);
        }
        return pipeline;
    }

    /**
     * Builds an element from a stage definition.
     *
     * @param def          the stage definition
     * @param pipelineName the parent pipeline name
     * @return the pipeline element
     */
    private PipelineElement buildElement(StageDef def, String pipelineName) {
        String type = def.getType() != null ? def.getType() : "stage";
        String name = def.getName() != null ? def.getName() : (def.getStage() != null ? def.getStage() : type);

        PipelineElement element;
        if ("llm-stage".equals(type)) {
            element = buildLlmStage(def, pipelineName);
        } else if (def.getStage() != null) {
            element = resolveLeafStage(def, pipelineName);
        } else if ("stage".equals(type)) {
            element = resolveLeafStage(def, pipelineName);
        } else {
            element = buildComposite(type, def, pipelineName);
        }

        element = hookManager.wrapWithHooks(element, name);
        return element;
    }

    /**
     * Resolves a leaf stage from the registry.
     *
     * @param def          the stage definition
     * @param pipelineName the parent pipeline name
     * @return the resolved pipeline element
     */
    private PipelineElement resolveLeafStage(StageDef def, String pipelineName) {
        String stageType = def.getStage() != null ? def.getStage() : def.getName();
        if (stageType == null) {
            throw new IllegalArgumentException(
                    "Stage definition missing 'stage' or 'name' in pipeline " + pipelineName);
        }

        PipelineElement stage = stageRegistry.getStage(stageType);

        boolean hasLlmConfig = def.getLlm() != null && !def.getLlm().isEmpty();
        if (hasLlmConfig) {
            Map<String, Object> llmConfig = def.getLlm();
            stage = new LlmConfiguringElement(stage, llmConfig);
        }

        return stage;
    }

    /**
     * Builds an LLM stage.
     *
     * @param def          the stage definition
     * @param pipelineName the parent pipeline name
     * @return the generic LLM stage
     */
    private PipelineElement buildLlmStage(StageDef def, String pipelineName) {
        String name = def.getName() != null ? def.getName() : "llm-stage";
        if (def.getSystemPrompt() == null || def.getSystemPrompt().isBlank()) {
            throw new IllegalArgumentException(
                    "llm-stage '" + name + "' requires 'system-prompt' in pipeline " + pipelineName);
        }
        return new GenericLlmStage(
                name,
                def.getSystemPrompt(),
                def.getUserPrompt(),
                def.getResponseSchema(),
                def.getOutputMapping(),
                def.getLlm()
        );
    }

    /**
     * Builds a composite element based on type.
     *
     * @param type         the composite type
     * @param def          the stage definition
     * @param pipelineName the parent pipeline name
     * @return the composite pipeline element
     */
    private PipelineElement buildComposite(String type, StageDef def, String pipelineName) {
        String name = def.getName() != null ? def.getName() : type;

        return switch (type) {
            case "sequence" -> buildSequence(name, def, pipelineName);
            case "parallel" -> buildParallel(name, def, pipelineName);
            case "fork-join" -> buildForkJoin(name, def, pipelineName);
            case "retry" -> buildRetry(name, def, pipelineName);
            case "conditional" -> buildConditional(name, def, pipelineName);
            case "switch" -> buildSwitch(name, def, pipelineName);
            case "timeout" -> buildTimeout(name, def, pipelineName);
            case "circuit-breaker" -> buildCircuitBreaker(name, def, pipelineName);
            default -> throw new IllegalArgumentException(
                    "Unknown composite type: " + type + " in pipeline " + pipelineName);
        };
    }

    /**
     * Builds a Sequence composite.
     *
     * @param name         the element name
     * @param def          the stage definition
     * @param pipelineName the parent pipeline name
     * @return the sequence
     */
    private Sequence buildSequence(String name, StageDef def, String pipelineName) {
        Sequence seq = new Sequence(name);
        if (def.getStages() != null) {
            for (StageDef child : def.getStages()) {
                seq.add(buildElement(child, pipelineName));
            }
        }
        return seq;
    }

    /**
     * Builds a Parallel composite.
     *
     * @param name         the element name
     * @param def          the stage definition
     * @param pipelineName the parent pipeline name
     * @return the parallel
     */
    private Parallel buildParallel(String name, StageDef def, String pipelineName) {
        Parallel parallel = new Parallel(name);
        if (def.getStages() != null) {
            for (StageDef child : def.getStages()) {
                parallel.add(buildElement(child, pipelineName));
            }
        }
        return parallel;
    }

    /**
     * Builds a ForkJoin composite.
     *
     * @param name         the element name
     * @param def          the stage definition
     * @param pipelineName the parent pipeline name
     * @return the fork-join
     */
    private ForkJoin buildForkJoin(String name, StageDef def, String pipelineName) {
        if (def.getStages() == null || def.getStages().isEmpty()) {
            throw new IllegalArgumentException(
                    "fork-join requires at least one fork stage in pipeline " + pipelineName);
        }
        List<StageDef> children = def.getStages();
        PipelineElement joinElement = buildElement(children.getLast(), pipelineName);
        ForkJoin fj = new ForkJoin(name, joinElement);
        for (int i = 0; i < children.size() - 1; i++) {
            fj.fork(buildElement(children.get(i), pipelineName));
        }
        return fj;
    }

    /**
     * Builds a Retry composite.
     *
     * @param name         the element name
     * @param def          the stage definition
     * @param pipelineName the parent pipeline name
     * @return the retry
     */
    private Retry buildRetry(String name, StageDef def, String pipelineName) {
        int retries = def.getRetries() > 0 ? def.getRetries() : 3;
        Retry retry = new Retry(name, retries);
        if (def.getStages() != null) {
            for (StageDef child : def.getStages()) {
                retry.add(buildElement(child, pipelineName));
            }
        }
        return retry;
    }

    /**
     * Builds a Conditional composite.
     *
     * @param name         the element name
     * @param def          the stage definition
     * @param pipelineName the parent pipeline name
     * @return the conditional
     */
    private Conditional buildConditional(String name, StageDef def, String pipelineName) {
        String conditionStr = def.getCondition();
        if (conditionStr == null || conditionStr.isBlank()) {
            throw new IllegalArgumentException(
                    "conditional requires 'condition' in pipeline " + pipelineName);
        }

        PipelineElement thenElement = resolveBranch(
                def.getStages(), def.getThen(), pipelineName, "then");
        PipelineElement elseElement = resolveBranch(
                def.getElseStages(), def.getElseBranch(), pipelineName, "else");

        return new Conditional(name, parseCondition(conditionStr), thenElement, elseElement);
    }

    /**
     * Resolves a branch for conditional execution.
     *
     * @param stages     the child stages
     * @param stageRef   the stage reference
     * @param pipelineName the parent pipeline name
     * @param branchName the branch name
     * @return the pipeline element for the branch, or null
     */
    private PipelineElement resolveBranch(
            List<StageDef> stages, String stageRef, String pipelineName, String branchName) {
        if (stages != null && !stages.isEmpty()) {
            Sequence seq = new Sequence(branchName + "-branch");
            for (StageDef s : stages) {
                seq.add(buildElement(s, pipelineName));
            }
            return seq;
        }
        if (stageRef != null) {
            return stageRegistry.getStage(stageRef);
        }
        return null;
    }

    /**
     * Parses a condition string into a predicate.
     *
     * @param condition the condition string
     * @return the predicate
     */
    private Predicate<PipelineContext> parseCondition(String condition) {
        String trimmed = condition.trim();
        if (trimmed.startsWith("!")) {
            String key = trimmed.substring(1).trim();
            return ctx -> !Boolean.TRUE.equals(ctx.getValue(createContextKey(key)));
        }
        if (trimmed.contains(" ")) {
            return ctx -> Boolean.TRUE.equals(ctx.getValue(createContextKey(trimmed)));
        }
        return ctx -> Boolean.TRUE.equals(ctx.getValue(createContextKey(trimmed)));
    }

    /**
     * Creates a context key for a condition variable.
     *
     * @param name the variable name
     * @return the context key
     */
    private ContextKey<Boolean> createContextKey(String name) {
        return new ContextKey<>(name.replace(".", "_"), Boolean.class);
    }

    /**
     * Builds a Switch composite.
     *
     * @param name         the element name
     * @param def          the stage definition
     * @param pipelineName the parent pipeline name
     * @return the switch
     */
    private Switch buildSwitch(String name, StageDef def, String pipelineName) {
        Switch sw = new Switch(name);
        if (def.getStages() != null) {
            for (StageDef child : def.getStages()) {
                if (child.getCondition() != null && child.getStage() != null) {
                    PipelineElement branch = stageRegistry.getStage(child.getStage());
                    sw.addCase(parseCondition(child.getCondition()), branch);
                }
            }
        }
        if (def.getStage() != null) {
            sw.defaultBranch(stageRegistry.getStage(def.getStage()));
        }
        return sw;
    }

    /**
     * Builds a Timeout composite.
     *
     * @param name         the element name
     * @param def          the stage definition
     * @param pipelineName the parent pipeline name
     * @return the timeout
     */
    private Timeout buildTimeout(String name, StageDef def, String pipelineName) {
        Duration timeout = def.getTimeoutMs() > 0
                ? Duration.ofMillis(def.getTimeoutMs()) : Duration.ofSeconds(30);
        if (def.getStage() != null) {
            return new Timeout(name, stageRegistry.getStage(def.getStage()), timeout);
        }
        if (def.getStages() != null && !def.getStages().isEmpty()) {
            Sequence seq = new Sequence(name + "-wrapped");
            for (StageDef child : def.getStages()) {
                seq.add(buildElement(child, pipelineName));
            }
            return new Timeout(name, seq, timeout);
        }
        throw new IllegalArgumentException(
                "timeout requires a 'stage' or 'stages' in pipeline " + pipelineName);
    }

    /**
     * Builds a CircuitBreaker composite.
     *
     * @param name         the element name
     * @param def          the stage definition
     * @param pipelineName the parent pipeline name
     * @return the circuit breaker
     */
    private CircuitBreaker buildCircuitBreaker(String name, StageDef def, String pipelineName) {
        int threshold = def.getFailureThreshold() > 0 ? def.getFailureThreshold() : 5;
        Duration reset = def.getResetTimeoutMs() > 0
                ? Duration.ofMillis(def.getResetTimeoutMs()) : Duration.ofSeconds(60);
        if (def.getStage() != null) {
            return new CircuitBreaker(
                    name, stageRegistry.getStage(def.getStage()), threshold, reset);
        }
        if (def.getStages() != null && !def.getStages().isEmpty()) {
            Sequence seq = new Sequence(name + "-wrapped");
            for (StageDef child : def.getStages()) {
                seq.add(buildElement(child, pipelineName));
            }
            return new CircuitBreaker(name, seq, threshold, reset);
        }
        throw new IllegalArgumentException(
                "circuit-breaker requires a 'stage' or 'stages' in pipeline " + pipelineName);
    }

    /**
     * Wrapper element that injects LLM configuration into the pipeline context before delegation.
     */
    private static class LlmConfiguringElement implements PipelineElement {

        /** The delegate element. */
        private final PipelineElement delegate;

        /** The LLM configuration to inject. */
        private final Map<String, Object> llmConfig;

        /**
         * Constructs a new LlmConfiguringElement.
         *
         * @param delegate  the delegate element
         * @param llmConfig the LLM configuration map
         */
        LlmConfiguringElement(PipelineElement delegate, Map<String, Object> llmConfig) {
            this.delegate = delegate;
            this.llmConfig = Map.copyOf(llmConfig);
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public List<PipelineListener> getListeners() {
            return delegate.getListeners();
        }

        @Override
        public void setListeners(List<PipelineListener> listeners) {
            delegate.setListeners(listeners);
        }

        @Override
        public boolean execute(PipelineContext context) throws Exception {
            context.putValue(ContextKeys.System.LLM_CONFIG, llmConfig);
            return delegate.execute(context);
        }
    }
}