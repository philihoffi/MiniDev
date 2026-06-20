package org.philipp.fun.minidev.pipeline.wallpaper.stages;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.philipp.fun.minidev.dto.WallpaperCode;
import org.philipp.fun.minidev.pipeline.annotation.PipelineStage;
import org.philipp.fun.minidev.pipeline.core.BaseElement;
import org.philipp.fun.minidev.pipeline.core.ContextKeys;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pipeline stage that assembles wallpaper HTML, CSS, and JS into a single
 * HTML document.
 */
@PipelineStage("assemble-html")
public class AssembleHtmlStage extends BaseElement {

    /** Shared ObjectMapper for JSON deserialization. */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** Logger for this class. */
    private static final Logger LOG = LoggerFactory.getLogger(AssembleHtmlStage.class);

    /** HTML template used for assembly. */
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

    /**
     * Constructs an AssembleHtmlStage.
     */
    public AssembleHtmlStage() {
        super("AssembleHtmlStage");
    }

    /**
     * Reads the wallpaper code from the context, parses it, and assembles the
     * final HTML.
     *
     * @param context the pipeline context
     * @return {@code true} if assembly succeeded
     * @throws Exception if an unexpected error occurs
     */
    @Override
    public boolean execute(PipelineContext context) throws Exception {
        String rawJson = context.getValue(ContextKeys.Wallpaper.CODE);
        if (rawJson == null || rawJson.isBlank()) {
            LOG.warn("No code to assemble");
            return false;
        }

        WallpaperCode response;
        try {
            response = OBJECT_MAPPER.readValue(rawJson, WallpaperCode.class);
        } catch (Exception e) {
            LOG.warn("Failed to parse code JSON for assembly: {}", e.getMessage());
            return false;
        }

        if (response.html() == null || response.html().isBlank()) {
            LOG.warn("HTML content is empty");
            return false;
        }
        if (response.js() == null || response.js().isBlank()) {
            LOG.warn("JavaScript content is empty");
            return false;
        }

        String fullHtml = ASSEMBLY_TEMPLATE
                .replace("%%CSS%%", response.css() != null ? response.css() : "")
                .replace("%%HTML%%", response.html())
                .replace("%%JS%%", response.js());

        context.putValue(ContextKeys.Wallpaper.CODE, fullHtml);
        LOG.info("Assembled wallpaper HTML ({} chars)", fullHtml.length());
        return true;
    }
}
