package org.philipp.fun.minidev.pipeline.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PipelineDefinition {
    private String name;
    private String description;
    private List<StageDef> stages = new ArrayList<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<StageDef> getStages() { return stages; }
    public void setStages(List<StageDef> stages) { this.stages = stages; }

    public static class StageDef {
        private String type;
        private String name;
        private String stage;
        private List<StageDef> stages;
        private List<StageDef> elseStages;
        private String condition;
        private String then;
        private String else_;
        private int retries;
        private int timeoutMs;
        private int failureThreshold;
        private int resetTimeoutMs;
        private Map<String, Object> llm = new LinkedHashMap<>();
        private Map<String, Object> config = new LinkedHashMap<>();

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getStage() { return stage; }
        public void setStage(String stage) { this.stage = stage; }
        public List<StageDef> getStages() { return stages; }
        public void setStages(List<StageDef> stages) { this.stages = stages; }
        public List<StageDef> getElseStages() { return elseStages; }
        public void setElseStages(List<StageDef> elseStages) { this.elseStages = elseStages; }
        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }
        public String getThen() { return then; }
        public void setThen(String then) { this.then = then; }
        public String getElse_() { return else_; }
        public void setElse_(String else_) { this.else_ = else_; }
        public int getRetries() { return retries; }
        public void setRetries(int retries) { this.retries = retries; }
        public int getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
        public int getFailureThreshold() { return failureThreshold; }
        public void setFailureThreshold(int failureThreshold) { this.failureThreshold = failureThreshold; }
        public int getResetTimeoutMs() { return resetTimeoutMs; }
        public void setResetTimeoutMs(int resetTimeoutMs) { this.resetTimeoutMs = resetTimeoutMs; }
        public Map<String, Object> getLlm() { return llm; }
        public void setLlm(Map<String, Object> llm) { this.llm = llm; }
        public Map<String, Object> getConfig() { return config; }
        public void setConfig(Map<String, Object> config) { this.config = config; }
    }
}