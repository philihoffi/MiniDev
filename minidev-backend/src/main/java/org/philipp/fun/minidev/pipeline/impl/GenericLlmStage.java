package org.philipp.fun.minidev.pipeline.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.philipp.fun.minidev.dto.llm.JsonSchema;
import org.philipp.fun.minidev.dto.llm.LlmRequest;
import org.philipp.fun.minidev.dto.llm.LlmResponse;
import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.pipeline.core.BaseElement;
import org.philipp.fun.minidev.pipeline.core.ContextKey;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.philipp.fun.minidev.pipeline.core.ContextKeys.System.LLM_CLIENT;

public class GenericLlmStage extends BaseElement {
    private static final Logger log = LoggerFactory.getLogger(GenericLlmStage.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");

    private final String systemPrompt;
    private final String userPromptTemplate;
    private final Map<String, Object> responseSchema;
    private final Map<String, String> outputMapping;
    private final Map<String, Object> llmConfig;

    public GenericLlmStage(String name, String systemPrompt, String userPromptTemplate,
                           Map<String, Object> responseSchema, Map<String, String> outputMapping,
                           Map<String, Object> llmConfig) {
        super(name);
        this.systemPrompt = systemPrompt;
        this.userPromptTemplate = userPromptTemplate;
        this.responseSchema = responseSchema;
        this.outputMapping = outputMapping;
        this.llmConfig = llmConfig;
    }

    @Override
    public boolean execute(PipelineContext context) throws Exception {
        LlmClient llmClient = context.getValue(LLM_CLIENT);
        if (llmClient == null) {
            log.warn("No LLM client available in context, skipping '{}'", getName());
            return false;
        }

        String renderedUserPrompt = renderTemplate(userPromptTemplate, context);

        JsonSchema schema = null;
        if (responseSchema != null && !responseSchema.isEmpty()) {
            schema = JsonSchema.defaultSchema(responseSchema);
        }

        Double temperature = null;
        if (llmConfig != null && llmConfig.containsKey("temperature")) {
            temperature = toDouble(llmConfig.get("temperature"));
        }

        List<LlmRequest.Message> messages = List.of(
                LlmRequest.Message.system(systemPrompt),
                LlmRequest.Message.user(renderedUserPrompt)
        );

        LlmRequest request = new LlmRequest(messages, temperature, null, schema, null, null);
        LlmResponse response = llmClient.chat(request);

        if (!response.success()) {
            log.warn("LLM call '{}' failed: {}", getName(), response.errorMessage());
            return false;
        }

        applyOutputMapping(response.content(), context);
        log.info("LLM stage '{}' completed ({} chars)", getName(), response.content().length());
        return true;
    }

    private String renderTemplate(String template, PipelineContext context) {
        if (template == null || template.isBlank()) return template;
        Matcher matcher = VAR_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            result.append(template, lastEnd, matcher.start());
            String varName = matcher.group(1);
            String value = context.entrySet().stream()
                    .filter(e -> e.getKey().name().equals(varName))
                    .findFirst()
                    .map(e -> e.getValue() != null ? e.getValue().toString() : "")
                    .orElse("{{" + varName + "}}");
            result.append(value);
            lastEnd = matcher.end();
        }
        result.append(template.substring(lastEnd));
        return result.toString();
    }

    private void applyOutputMapping(String responseContent, PipelineContext context) {
        if (outputMapping == null || outputMapping.isEmpty()) return;

        try {
            JsonNode root = OBJECT_MAPPER.readTree(responseContent);

            for (var entry : outputMapping.entrySet()) {
                String contextKeyName = entry.getKey();
                String jsonPath = entry.getValue();
                String value;

                if ("$response".equals(jsonPath)) {
                    value = responseContent;
                } else if (jsonPath != null && jsonPath.startsWith("$.")) {
                    String field = jsonPath.substring(2);
                    value = resolveJsonPath(root, field);
                } else {
                    value = jsonPath;
                }

                if (value != null) {
                    context.putValue(new ContextKey<>(contextKeyName, String.class), value);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to apply output mapping for '{}': {}", getName(), e.getMessage());
        }
    }

    private static String resolveJsonPath(JsonNode root, String path) {
        String[] parts = path.split("\\.");
        JsonNode node = root;
        for (String part : parts) {
            if (node == null) return null;
            node = node.get(part);
        }
        if (node == null) return null;
        if (node.isTextual()) return node.asText();
        if (node.isBoolean()) return Boolean.toString(node.asBoolean());
        if (node.isNumber()) return node.asText();
        if (node.isArray()) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < node.size(); i++) {
                if (i > 0) sb.append(", ");
                JsonNode item = node.get(i);
                sb.append(item.isTextual() ? item.asText() : item.toString());
            }
            sb.append("]");
            return sb.toString();
        }
        return node.toString();
    }

    private static Double toDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }
}