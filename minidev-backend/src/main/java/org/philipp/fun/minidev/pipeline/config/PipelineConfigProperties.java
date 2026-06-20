package org.philipp.fun.minidev.pipeline.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for pipeline definitions loaded from
 * {@code minidev.pipeline.*}.
 */
@Configuration
@ConfigurationProperties(prefix = "minidev.pipeline")
public class PipelineConfigProperties {

    /** The list of configured pipeline definitions. */
    private List<PipelineDefinition> pipelines = new ArrayList<>();

    /**
     * Returns the list of pipeline definitions.
     *
     * @return the pipeline definitions
     */
    public List<PipelineDefinition> getPipelines() {
        return pipelines;
    }

    /**
     * Sets the list of pipeline definitions.
     *
     * @param pipelines the pipeline definitions
     */
    public void setPipelines(List<PipelineDefinition> pipelines) {
        this.pipelines = pipelines;
    }
}