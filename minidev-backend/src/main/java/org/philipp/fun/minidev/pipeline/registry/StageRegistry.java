package org.philipp.fun.minidev.pipeline.registry;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.philipp.fun.minidev.pipeline.annotation.PipelineStage;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Registry for pipeline stage beans discovered via annotation scanning.
 */
@Component
public class StageRegistry {

    /** Map of stage type to pipeline element. */
    private final Map<String, PipelineElement> stages = new ConcurrentHashMap<>();

    /**
     * Constructs a StageRegistry by scanning the application context.
     *
     * @param ctx the application context
     */
    public StageRegistry(ApplicationContext ctx) {
        Map<String, Object> beans = ctx.getBeansWithAnnotation(PipelineStage.class);
        for (var entry : beans.entrySet()) {
            Object bean = entry.getValue();
            if (!(bean instanceof PipelineElement element)) {
                continue;
            }
            PipelineStage ann = bean.getClass().getAnnotation(PipelineStage.class);
            String stageType = ann.value();
            if (stageType == null || stageType.isBlank()) {
                stageType = element.getName();
            }
            stages.put(stageType, element);
        }
    }

    /**
     * Returns the pipeline element for a given stage type.
     *
     * @param stageType the stage type
     * @return the pipeline element
     * @throws IllegalArgumentException if the stage type is unknown
     */
    public PipelineElement getStage(String stageType) {
        PipelineElement stage = stages.get(stageType);
        if (stage == null) {
            throw new IllegalArgumentException("Unknown pipeline stage: " + stageType
                    + ". Available stages: " + stages.keySet());
        }
        return stage;
    }

    /**
     * Returns whether the registry contains the given stage type.
     *
     * @param stageType the stage type
     * @return true if the stage is registered
     */
    public boolean containsStage(String stageType) {
        return stages.containsKey(stageType);
    }

    /**
     * Returns an unmodifiable view of all registered stages.
     *
     * @return map of stage types to pipeline elements
     */
    public Map<String, PipelineElement> getAllStages() {
        return Collections.unmodifiableMap(stages);
    }

    /**
     * Registers a stage manually.
     *
     * @param stageType the stage type
     * @param element   the pipeline element
     */
    public void register(String stageType, PipelineElement element) {
        stages.put(stageType, element);
    }
}