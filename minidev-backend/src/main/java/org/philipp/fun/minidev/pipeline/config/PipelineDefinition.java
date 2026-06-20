package org.philipp.fun.minidev.pipeline.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Describes a pipeline with a name, description, and ordered list of stage definitions.
 */
public class PipelineDefinition {

    /** The pipeline name. */
    private String name;

    /** The pipeline description. */
    private String description;

    /** The top-level stage definitions. */
    private List<StageDef> stages = new ArrayList<>();

    /**
     * Gets the pipeline name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the pipeline name.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the pipeline description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the pipeline description.
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the top-level stage definitions.
     *
     * @return the stages
     */
    public List<StageDef> getStages() {
        return stages;
    }

    /**
     * Sets the top-level stage definitions.
     *
     * @param stages the stages to set
     */
    public void setStages(List<StageDef> stages) {
        this.stages = stages;
    }

    /**
     * Defines a single stage within a pipeline, supporting composite and leaf types.
     */
    public static class StageDef {

        /** The stage type (e.g. sequence, parallel, llm-stage, stage). */
        private String type;

        /** The stage name. */
        private String name;

        /** Reference to a registered leaf stage. */
        private String stage;

        /** Child stage definitions for composites. */
        private List<StageDef> stages;

        /** Alternative child stages for conditional else-branch. */
        private List<StageDef> elseStages;

        /** Condition expression for conditional/switch. */
        private String condition;

        /** Reference to the then-branch stage. */
        private String then;

        /** Reference to the else-branch stage. */
        private String elseBranch;

        /** Number of retries for retry composite. */
        private int retries;

        /** Timeout in milliseconds for timeout composite. */
        private int timeoutMs;

        /** Failure threshold for circuit breaker. */
        private int failureThreshold;

        /** Reset timeout in milliseconds for circuit breaker. */
        private int resetTimeoutMs;

        /** LLM-specific configuration. */
        private Map<String, Object> llm = new LinkedHashMap<>();

        /** Generic stage configuration. */
        private Map<String, Object> config = new LinkedHashMap<>();

        /** The system prompt for LLM stages. */
        private String systemPrompt;

        /** The user prompt template for LLM stages. */
        private String userPrompt;

        /** JSON schema for structured LLM output. */
        private Map<String, Object> responseSchema;

        /** Mapping from context keys to output paths. */
        private Map<String, String> outputMapping;

        /**
         * Gets the stage type.
         *
         * @return the type
         */
        public String getType() {
            return type;
        }

        /**
         * Sets the stage type.
         *
         * @param type the type to set
         */
        public void setType(String type) {
            this.type = type;
        }

        /**
         * Gets the stage name.
         *
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the stage name.
         *
         * @param name the name to set
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Gets the referenced leaf stage.
         *
         * @return the stage reference
         */
        public String getStage() {
            return stage;
        }

        /**
         * Sets the referenced leaf stage.
         *
         * @param stage the stage reference to set
         */
        public void setStage(String stage) {
            this.stage = stage;
        }

        /**
         * Gets the child stage definitions.
         *
         * @return the children
         */
        public List<StageDef> getStages() {
            return stages;
        }

        /**
         * Sets the child stage definitions.
         *
         * @param stages the children to set
         */
        public void setStages(List<StageDef> stages) {
            this.stages = stages;
        }

        /**
         * Gets the else-branch stage definitions.
         *
         * @return the else stages
         */
        public List<StageDef> getElseStages() {
            return elseStages;
        }

        /**
         * Sets the else-branch stage definitions.
         *
         * @param elseStages the else stages to set
         */
        public void setElseStages(List<StageDef> elseStages) {
            this.elseStages = elseStages;
        }

        /**
         * Gets the condition expression.
         *
         * @return the condition
         */
        public String getCondition() {
            return condition;
        }

        /**
         * Sets the condition expression.
         *
         * @param condition the condition to set
         */
        public void setCondition(String condition) {
            this.condition = condition;
        }

        /**
         * Gets the then-branch stage reference.
         *
         * @return the then reference
         */
        public String getThen() {
            return then;
        }

        /**
         * Sets the then-branch stage reference.
         *
         * @param then the then reference to set
         */
        public void setThen(String then) {
            this.then = then;
        }

        /**
         * Gets the else-branch stage reference.
         *
         * @return the else reference
         */
        public String getElseBranch() {
            return elseBranch;
        }

        /**
         * Sets the else-branch stage reference.
         *
         * @param elseBranch the else reference to set
         */
        public void setElseBranch(String elseBranch) {
            this.elseBranch = elseBranch;
        }

        /**
         * Gets the retry count.
         *
         * @return the retries
         */
        public int getRetries() {
            return retries;
        }

        /**
         * Sets the retry count.
         *
         * @param retries the retries to set
         */
        public void setRetries(int retries) {
            this.retries = retries;
        }

        /**
         * Gets the timeout in milliseconds.
         *
         * @return the timeout ms
         */
        public int getTimeoutMs() {
            return timeoutMs;
        }

        /**
         * Sets the timeout in milliseconds.
         *
         * @param timeoutMs the timeout ms to set
         */
        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        /**
         * Gets the failure threshold for circuit breaker.
         *
         * @return the failure threshold
         */
        public int getFailureThreshold() {
            return failureThreshold;
        }

        /**
         * Sets the failure threshold for circuit breaker.
         *
         * @param failureThreshold the failure threshold to set
         */
        public void setFailureThreshold(int failureThreshold) {
            this.failureThreshold = failureThreshold;
        }

        /**
         * Gets the reset timeout in milliseconds for circuit breaker.
         *
         * @return the reset timeout ms
         */
        public int getResetTimeoutMs() {
            return resetTimeoutMs;
        }

        /**
         * Sets the reset timeout in milliseconds for circuit breaker.
         *
         * @param resetTimeoutMs the reset timeout ms to set
         */
        public void setResetTimeoutMs(int resetTimeoutMs) {
            this.resetTimeoutMs = resetTimeoutMs;
        }

        /**
         * Gets the LLM configuration.
         *
         * @return the LLM config
         */
        public Map<String, Object> getLlm() {
            return llm;
        }

        /**
         * Sets the LLM configuration.
         *
         * @param llm the LLM config to set
         */
        public void setLlm(Map<String, Object> llm) {
            this.llm = llm;
        }

        /**
         * Gets the generic stage configuration.
         *
         * @return the config
         */
        public Map<String, Object> getConfig() {
            return config;
        }

        /**
         * Sets the generic stage configuration.
         *
         * @param config the config to set
         */
        public void setConfig(Map<String, Object> config) {
            this.config = config;
        }

        /**
         * Gets the system prompt for LLM stages.
         *
         * @return the system prompt
         */
        public String getSystemPrompt() {
            return systemPrompt;
        }

        /**
         * Sets the system prompt for LLM stages.
         *
         * @param systemPrompt the system prompt to set
         */
        public void setSystemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
        }

        /**
         * Gets the user prompt template for LLM stages.
         *
         * @return the user prompt
         */
        public String getUserPrompt() {
            return userPrompt;
        }

        /**
         * Sets the user prompt template for LLM stages.
         *
         * @param userPrompt the user prompt to set
         */
        public void setUserPrompt(String userPrompt) {
            this.userPrompt = userPrompt;
        }

        /**
         * Gets the JSON response schema for structured LLM output.
         *
         * @return the response schema
         */
        public Map<String, Object> getResponseSchema() {
            return responseSchema;
        }

        /**
         * Sets the JSON response schema for structured LLM output.
         *
         * @param responseSchema the response schema to set
         */
        public void setResponseSchema(Map<String, Object> responseSchema) {
            this.responseSchema = responseSchema;
        }

        /**
         * Gets the output mapping from context keys to response paths.
         *
         * @return the output mapping
         */
        public Map<String, String> getOutputMapping() {
            return outputMapping;
        }

        /**
         * Sets the output mapping from context keys to response paths.
         *
         * @param outputMapping the output mapping to set
         */
        public void setOutputMapping(Map<String, String> outputMapping) {
            this.outputMapping = outputMapping;
        }
    }
}