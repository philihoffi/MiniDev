package org.philipp.fun.minidev.pipeline.registry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.philipp.fun.minidev.pipeline.config.PipelineConfigProperties;
import org.philipp.fun.minidev.pipeline.config.PipelineDefinition;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Registry that holds all built pipeline instances keyed by name.
 */
@Component
@DependsOn("pipelineConfigLoader")
public class PipelineRegistry {

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(PipelineRegistry.class);

    /** Map of registered pipelines. */
    private final Map<String, PipelineElement> pipelines = new LinkedHashMap<>();

    /** Factory for building pipelines from definitions. */
    private final PipelineFactory pipelineFactory;

    /** Configuration properties containing pipeline definitions. */
    private final PipelineConfigProperties configProperties;

    /**
     * Constructs a new PipelineRegistry.
     *
     * @param pipelineFactory  the pipeline factory
     * @param configProperties the pipeline config properties
     */
    public PipelineRegistry(
            PipelineFactory pipelineFactory,
            PipelineConfigProperties configProperties) {
        this.pipelineFactory = pipelineFactory;
        this.configProperties = configProperties;
    }

    /**
     * Initializes the registry by building all pipelines from configuration.
     */
    @PostConstruct
    public void initialize() {
        for (PipelineDefinition def : configProperties.getPipelines()) {
            String name = def.getName();
            if (name == null || name.isBlank()) {
                LOG.warn("Skipping pipeline definition without name");
                continue;
            }
            try {
                PipelineElement pipeline = pipelineFactory.createPipeline(def);
                pipelines.put(name, pipeline);
                LOG.info(
                        "Registered pipeline: '{}' with {} top-level stages",
                        name, def.getStages().size());
            } catch (Exception e) {
                LOG.error(
                        "Failed to build pipeline '{}': {}",
                        name, e.getMessage(), e);
            }
        }
    }

    /**
     * Gets a pipeline by name.
     *
     * @param name the pipeline name
     * @return the pipeline element
     * @throws IllegalArgumentException if the pipeline is not found
     */
    public PipelineElement getPipeline(String name) {
        PipelineElement pipeline = pipelines.get(name);
        if (pipeline == null) {
            throw new IllegalArgumentException(
                    "Unknown pipeline: " + name
                    + ". Available pipelines: " + pipelines.keySet());
        }
        return pipeline;
    }

    /**
     * Checks whether a pipeline is registered.
     *
     * @param name the pipeline name
     * @return true if the pipeline exists
     */
    public boolean containsPipeline(String name) {
        return pipelines.containsKey(name);
    }

    /**
     * Returns an unmodifiable view of all registered pipelines.
     *
     * @return the pipelines map
     */
    public Map<String, PipelineElement> getAllPipelines() {
        return Collections.unmodifiableMap(pipelines);
    }

    /**
     * Registers a pipeline element under the given name.
     *
     * @param name     the pipeline name
     * @param pipeline the pipeline element
     */
    public void register(String name, PipelineElement pipeline) {
        pipelines.put(name, pipeline);
    }
}