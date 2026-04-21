package org.philipp.fun.minidev.pipeline.pipelines.wallpaperPipeline.stages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.philipp.fun.minidev.pipeline.abstracts.AbstractStep;
import org.philipp.fun.minidev.pipeline.core.ContextKeys;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;

public class ValidateCodeStage extends AbstractStep {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public ValidateCodeStage() {
        super("ValidateCodeStage");
    }

    @Override
    public boolean execute(PipelineContext context) throws Exception {
        String rawJson = context.getValue(ContextKeys.WallpaperPipeline.GENERATED_CODE);
        if (rawJson == null || rawJson.isBlank()) {
            return false;
        }

        WallpaperResponse response = OBJECT_MAPPER.readValue(rawJson, WallpaperResponse.class);

        String fullHtml = String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <style>
                    body { margin: 0; overflow: hidden; background: #000; width: 100vw; height: 100vh; }
                    %s
                </style>
            </head>
            <body>
                %s
                <script>
                    try {
                        %s
                    } catch (e) {
                        console.error('Wallpaper Animation Error:', e);
                    }
                </script>
            </body>
            </html>
            """, response.css(), response.html(), response.js());

        context.putValue(ContextKeys.WallpaperPipeline.GENERATED_CODE, fullHtml);

        return true;
    }

    /**
     * Internal record to match the JSON schema used in CodeGeneratorStage.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WallpaperResponse(String html, String css, String js, String description) {}
}
