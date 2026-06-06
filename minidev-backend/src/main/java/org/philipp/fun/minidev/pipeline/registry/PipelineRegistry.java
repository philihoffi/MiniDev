package org.philipp.fun.minidev.pipeline.registry;

import org.philipp.fun.minidev.pipeline.core.PipelineElement;
import org.philipp.fun.minidev.pipeline.config.PipelineConfigProperties;
import org.philipp.fun.minidev.pipeline.config.PipelineDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@DependsOn("pipelineConfigLoader")
public class PipelineRegistry {
    private static final Logger log = LoggerFactory.getLogger(PipelineRegistry.class);
    private final Map<String, PipelineElement> pipelines = new LinkedHashMap<>();
    private final PipelineFactory pipelineFactory;
    private final PipelineConfigProperties configProperties;

    public PipelineRegistry(PipelineFactory pipelineFactory, PipelineConfigProperties configProperties) {
        this.pipelineFactory = pipelineFactory;
        this.configProperties = configProperties;
    }

    @PostConstruct
    public void initialize() {
        for (PipelineDefinition def : configProperties.getPipelines()) {
            String name = def.getName();
            if (name == null || name.isBlank()) {
                log.warn("Skipping pipeline definition without name");
                continue;
            }
            try {
                PipelineElement pipeline = pipelineFactory.createPipeline(def);
                pipelines.put(name, pipeline);
                log.info("Registered pipeline: '{}' with {} top-level stages", name, def.getStages().size());
            } catch (Exception e) {
                log.error("Failed to build pipeline '{}': {}", name, e.getMessage(), e);
            }
        }
    }

    public PipelineElement getPipeline(String name) {
        PipelineElement pipeline = pipelines.get(name);
        if (pipeline == null) {
            throw new IllegalArgumentException("Unknown pipeline: " + name
                    + ". Available pipelines: " + pipelines.keySet());
        }
        return pipeline;
    }

    public boolean containsPipeline(String name) {
        return pipelines.containsKey(name);
    }

    public Map<String, PipelineElement> getAllPipelines() {
        return Collections.unmodifiableMap(pipelines);
    }

    public void register(String name, PipelineElement pipeline) {
        pipelines.put(name, pipeline);
    }
}