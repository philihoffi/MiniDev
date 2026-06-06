package org.philipp.fun.minidev.pipeline.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "minidev.pipeline")
public class PipelineConfigProperties {
    private List<PipelineDefinition> pipelines = new ArrayList<>();

    public List<PipelineDefinition> getPipelines() { return pipelines; }
    public void setPipelines(List<PipelineDefinition> pipelines) { this.pipelines = pipelines; }
}