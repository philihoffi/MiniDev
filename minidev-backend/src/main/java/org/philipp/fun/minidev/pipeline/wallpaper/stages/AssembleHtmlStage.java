package org.philipp.fun.minidev.pipeline.wallpaper.stages;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.philipp.fun.minidev.dto.WallpaperCode;
import org.philipp.fun.minidev.pipeline.BaseElement;
import org.philipp.fun.minidev.pipeline.ContextKeys;
import org.philipp.fun.minidev.pipeline.PipelineContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AssembleHtmlStage extends BaseElement {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(AssembleHtmlStage.class);

    public AssembleHtmlStage() {
        super("AssembleHtmlStage");
    }

    @Override
    public boolean execute(PipelineContext context) throws Exception {
        String rawJson = context.getValue(ContextKeys.Wallpaper.CODE);
        if (rawJson == null || rawJson.isBlank()) {
            log.warn("No code to assemble");
            return false;
        }

        WallpaperCode response;
        try {
            response = OBJECT_MAPPER.readValue(rawJson, WallpaperCode.class);
        } catch (Exception e) {
            log.warn("Failed to parse code JSON for assembly: {}", e.getMessage());
            return false;
        }

        if (response.html() == null || response.html().isBlank()) {
            log.warn("HTML content is empty");
            return false;
        }
        if (response.js() == null || response.js().isBlank()) {
            log.warn("JavaScript content is empty");
            return false;
        }

        String fullHtml = ASSEMBLY_TEMPLATE
                .replace("%%CSS%%", response.css() != null ? response.css() : "")
                .replace("%%HTML%%", response.html())
                .replace("%%JS%%", response.js());

        context.putValue(ContextKeys.Wallpaper.CODE, fullHtml);
        log.info("Assembled wallpaper HTML ({} chars)", fullHtml.length());
        return true;
    }

    private static final String ASSEMBLY_TEMPLATE = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body { width: 100%; height: 100%; overflow: hidden; background: #000; }
                    canvas { display: block; }
                    %%CSS%%
                </style>
            </head>
            <body>
                %%HTML%%
                <script>
                    (function() {
                        'use strict';
                        try {
                            %%JS%%
                        } catch (e) {
                            console.error('Wallpaper Error:', e);
                        }
                    })();
                </script>
            </body>
            </html>
            """;
}
