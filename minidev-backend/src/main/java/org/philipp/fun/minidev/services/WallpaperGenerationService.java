package org.philipp.fun.minidev.services;

import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.pipelines.wallpaperPipeline.WallPaperPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WallpaperGenerationService {

    private static final Logger log = LoggerFactory.getLogger(WallpaperGenerationService.class);

    private final WallPaperPipeline wallPaperPipeline;
    private final PipelineProgressSseService pipelineProgressSseService;

    public WallpaperGenerationService(
            WallPaperPipeline wallPaperPipeline,
            PipelineProgressSseService pipelineProgressSseService
    ) {
        this.wallPaperPipeline = wallPaperPipeline;
        this.pipelineProgressSseService = pipelineProgressSseService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean generateNewWallpaperInNewTransaction() {
        PipelineContext context = new PipelineContext();
        context.setPipeline(wallPaperPipeline);

        String runId = UUID.randomUUID().toString();
        PipelineProgressListener listener = new PipelineProgressListener(
                runId,
                wallPaperPipeline.getName(),
                wallPaperPipeline,
                pipelineProgressSseService
        );

        wallPaperPipeline.setListeners(List.of(listener));
        pipelineProgressSseService.startRun(runId, wallPaperPipeline.getName());
        listener.markRootStarted();

        try {
            boolean success = wallPaperPipeline.execute(context);
            listener.markRootFinished(success);
            pipelineProgressSseService.finishRun(runId, wallPaperPipeline.getName(), success);

            if (!success) {
                log.error("Failed to generate wallpaper: Pipeline execution returned false");
            }
            return success;
        } catch (Exception e) {
            listener.markRootFinished(false);
            pipelineProgressSseService.finishRun(runId, wallPaperPipeline.getName(), false);
            log.error("Error during wallpaper generation: {}", e.getMessage(), e);
            return false;
        } finally {
            wallPaperPipeline.setListeners(List.of());
        }
    }
}
