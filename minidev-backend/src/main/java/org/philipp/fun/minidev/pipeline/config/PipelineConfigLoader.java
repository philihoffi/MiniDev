package org.philipp.fun.minidev.pipeline.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import jakarta.annotation.PostConstruct;

/**
 * Loads pipeline configuration from YAML files.
 */
@Component
public class PipelineConfigLoader {

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(PipelineConfigLoader.class);

    /** Pipeline configuration properties. */
    private final PipelineConfigProperties configProperties;

    /**
     * Constructs a PipelineConfigLoader.
     *
     * @param configProperties the pipeline config properties
     */
    public PipelineConfigLoader(PipelineConfigProperties configProperties) {
        this.configProperties = configProperties;
    }

    /**
     * Loads pipeline configs from classpath YAML files.
     */
    @PostConstruct
    public void loadPipelineConfigs() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:pipelines/*.yml");
            if (resources.length == 0) {
                LOG.debug("No pipeline config files found in classpath:pipelines/");
                return;
            }

            List<PipelineDefinition> loaded = new ArrayList<>();
            LoaderOptions loaderOptions = new LoaderOptions();
            loaderOptions.setMaxAliasesForCollections(10);
            Yaml yaml = new Yaml(loaderOptions);

            for (Resource resource : resources) {
                try {
                    Map<String, Object> raw = yaml.load(resource.getInputStream());
                    if (raw == null) {
                        continue;
                    }
                    List<Map<String, Object>> pipelineMaps =
                            (List<Map<String, Object>>) raw.get("pipelines");
                    if (pipelineMaps == null) {
                        continue;
                    }

                    for (Map<String, Object> pm : pipelineMaps) {
                        PipelineDefinition def = mapPipeline(pm);
                        if (def != null) {
                            loaded.add(def);
                        }
                    }
                    LOG.info("Loaded {} pipeline(s) from {}", pipelineMaps.size(),
                            resource.getFilename());
                } catch (Exception e) {
                    LOG.warn("Failed to load pipeline config from {}: {}",
                            resource.getFilename(), e.getMessage());
                }
            }

            if (!loaded.isEmpty()) {
                configProperties.getPipelines().addAll(loaded);
                LOG.info("Total pipeline definitions loaded: {}",
                        configProperties.getPipelines().size());
            }
        } catch (Exception e) {
            LOG.warn("Could not scan for pipeline configs: {}", e.getMessage());
        }
    }

    /**
     * Maps a raw YAML map to a PipelineDefinition.
     *
     * @param map the raw map
     * @return the pipeline definition
     */
    @SuppressWarnings("unchecked")
    private PipelineDefinition mapPipeline(Map<String, Object> map) {
        PipelineDefinition def = new PipelineDefinition();
        def.setName((String) map.get("name"));
        def.setDescription((String) map.get("description"));
        List<Map<String, Object>> stageMaps = (List<Map<String, Object>>) map.get("stages");
        if (stageMaps != null) {
            for (Map<String, Object> sm : stageMaps) {
                def.getStages().add(mapStageDef(sm));
            }
        }
        return def;
    }

    /**
     * Maps a raw YAML map to a StageDef.
     *
     * @param map the raw map
     * @return the stage definition
     */
    @SuppressWarnings("unchecked")
    private PipelineDefinition.StageDef mapStageDef(Map<String, Object> map) {
        PipelineDefinition.StageDef def = new PipelineDefinition.StageDef();
        def.setType((String) map.get("type"));
        def.setName((String) map.get("name"));
        def.setStage((String) map.get("stage"));

        if (map.containsKey("condition")) {
            def.setCondition((String) map.get("condition"));
        }
        if (map.containsKey("retries")) {
            def.setRetries(toInt(map.get("retries")));
        }
        if (map.containsKey("timeoutMs")) {
            def.setTimeoutMs(toInt(map.get("timeoutMs")));
        }
        if (map.containsKey("failureThreshold")) {
            def.setFailureThreshold(toInt(map.get("failureThreshold")));
        }
        if (map.containsKey("resetTimeoutMs")) {
            def.setResetTimeoutMs(toInt(map.get("resetTimeoutMs")));
        }

        if (map.containsKey("then") && map.get("then") instanceof String thenStr) {
            def.setThen(thenStr);
        }

        if (map.containsKey("else")) {
            Object elseObj = map.get("else");
            if (elseObj instanceof String elseStr) {
                def.setElseBranch(elseStr);
            } else if (elseObj instanceof List) {
                List<Map<String, Object>> elseStageMaps =
                        (List<Map<String, Object>>) elseObj;
                def.setElseStages(elseStageMaps.stream().map(this::mapStageDef).toList());
            }
        }

        if (map.containsKey("stages")) {
            List<Map<String, Object>> stageMaps =
                    (List<Map<String, Object>>) map.get("stages");
            def.setStages(stageMaps.stream().map(this::mapStageDef).toList());
        }

        if (map.containsKey("llm")) {
            def.setLlm((Map<String, Object>) map.get("llm"));
        }

        if (map.containsKey("system-prompt")) {
            def.setSystemPrompt((String) map.get("system-prompt"));
        }

        if (map.containsKey("user-prompt")) {
            def.setUserPrompt((String) map.get("user-prompt"));
        }

        if (map.containsKey("response-schema")) {
            def.setResponseSchema((Map<String, Object>) map.get("response-schema"));
        }

        if (map.containsKey("output-mapping")) {
            Map<String, Object> rawMapping = (Map<String, Object>) map.get("output-mapping");
            Map<String, String> mapping = new java.util.LinkedHashMap<>();
            for (var entry : rawMapping.entrySet()) {
                mapping.put(entry.getKey(),
                        entry.getValue() != null ? entry.getValue().toString() : null);
            }
            def.setOutputMapping(mapping);
        }

        return def;
    }

    /**
     * Converts an object to an integer.
     *
     * @param val the value to convert
     * @return the integer value, or 0 if not a number
     */
    private int toInt(Object val) {
        if (val instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }
}