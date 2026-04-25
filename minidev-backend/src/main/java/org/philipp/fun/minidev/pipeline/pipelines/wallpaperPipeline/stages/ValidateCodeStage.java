package org.philipp.fun.minidev.pipeline.pipelines.wallpaperPipeline.stages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.philipp.fun.minidev.dto.llm.JsonSchema;
import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.dto.llm.LlmRequest;
import org.philipp.fun.minidev.dto.llm.LlmResponse;
import org.philipp.fun.minidev.pipeline.abstracts.AbstractStep;
import org.philipp.fun.minidev.pipeline.core.ContextKeys;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static org.philipp.fun.minidev.pipeline.core.ContextKeys.System.LLM_CLIENT;

@Component
public class ValidateCodeStage extends AbstractStep {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(ValidateCodeStage.class);

    public ValidateCodeStage() {
        super("ValidateCodeStage");
    }

    @Override
    public boolean execute(PipelineContext context) throws Exception {
        String rawJson = context.getValue(ContextKeys.WallpaperPipeline.GENERATED_CODE);
        if (rawJson == null || rawJson.isBlank()) {
            return false;
        }

        WallpaperResponse response;
        try {
            response = OBJECT_MAPPER.readValue(rawJson, WallpaperResponse.class);
        } catch (Exception e) {
            return false;
        }

        if (response.html() == null || response.html().isBlank() ||
            response.js() == null || response.js().isBlank()) {
            return false;
        }

        // LLM Validation
        if (!validateWithLlm(context, response)) {
            return false;
        }

        String fullHtml = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <style>
                    body { margin: 0; overflow: hidden; background: #000; width: 100vw; height: 100vh; }
                    {{CSS}}
                </style>
            </head>
            <body>
                {{HTML}}
                <script>
                    try {
                        {{JS}}
                    } catch (e) {
                        console.error('Wallpaper Animation Error:', e);
                    }
                </script>
            </body>
            </html>
            """
            .replace("{{CSS}}", response.css() != null ? response.css() : "")
            .replace("{{HTML}}", response.html())
            .replace("{{JS}}", response.js());

        context.putValue(ContextKeys.WallpaperPipeline.GENERATED_CODE, fullHtml);

        return true;
    }

    private boolean validateWithLlm(PipelineContext context, WallpaperResponse response) {
        LlmClient llmClient = context.getValue(LLM_CLIENT);
        if (llmClient == null) {
            log.debug("No LLM client available, skipping validation");
            return true; // Skip if no LLM client
        }

        JsonSchema schema = JsonSchema.defaultSchema(Map.of(
            "type", "object",
            "properties", Map.of(
                "is_broken", Map.of("type", "boolean", "description", "True if the code is broken, non-functional, or significantly low quality"),
                "reason", Map.of("type", "string", "description", "The reason why the code is considered broken")
            ),
            "required", List.of("is_broken", "reason")
        ));

        List<LlmRequest.Message> messages = List.of(
            LlmRequest.Message.system("You are an expert web developer and quality assurance engineer. " +
                "Your task is to validate if the provided HTML5 Canvas wallpaper code is functional and of high quality. " +
                "Check for: " +
                "1. Missing variables or functions. " +
                "2. Syntax errors in JS/CSS. " +
                "3. Logic that would prevent animation (e.g., requestAnimationFrame never called). " +
                "4. Empty or placeholder content. " +
                "5. Major performance issues. " +
                "Return a JSON object indicating if it's broken."),
            LlmRequest.Message.user(String.format("Validate this wallpaper code:\nHTML: %s\nCSS: %s\nJS: %s",
                response.html(), response.css(), response.js()))
        );

        LlmRequest request = new LlmRequest(messages, null, null, schema, null, null);
        LlmResponse llmResponse = llmClient.chat(request);

        if (!llmResponse.success()) {
            return true;
        }

        try {
            LlmValidationResult result = OBJECT_MAPPER.readValue(llmResponse.content(), LlmValidationResult.class);
            return !result.is_broken();
        } catch (Exception e) {
            return true;
        }
    }

    public record LlmValidationResult(boolean is_broken, String reason) {}

    /**
     * Internal record to match the JSON schema used in CodeGeneratorStage.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WallpaperResponse(String html, String css, String js, String description) {}
}
