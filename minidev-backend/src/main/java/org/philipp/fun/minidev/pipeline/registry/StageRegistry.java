package org.philipp.fun.minidev.pipeline.registry;

import org.philipp.fun.minidev.pipeline.core.PipelineElement;
import org.philipp.fun.minidev.pipeline.annotation.PipelineStage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StageRegistry {
    private final Map<String, PipelineElement> stages = new ConcurrentHashMap<>();

    public StageRegistry(ApplicationContext ctx) {
        Map<String, Object> beans = ctx.getBeansWithAnnotation(PipelineStage.class);
        for (var entry : beans.entrySet()) {
            Object bean = entry.getValue();
            if (!(bean instanceof PipelineElement element)) continue;
            PipelineStage ann = bean.getClass().getAnnotation(PipelineStage.class);
            String stageType = ann.value();
            if (stageType == null || stageType.isBlank()) {
                stageType = element.getName();
            }
            stages.put(stageType, element);
        }
    }

    public PipelineElement getStage(String stageType) {
        PipelineElement stage = stages.get(stageType);
        if (stage == null) {
            throw new IllegalArgumentException("Unknown pipeline stage: " + stageType
                    + ". Available stages: " + stages.keySet());
        }
        return stage;
    }

    public boolean containsStage(String stageType) {
        return stages.containsKey(stageType);
    }

    public Map<String, PipelineElement> getAllStages() {
        return Collections.unmodifiableMap(stages);
    }

    public void register(String stageType, PipelineElement element) {
        stages.put(stageType, element);
    }
}