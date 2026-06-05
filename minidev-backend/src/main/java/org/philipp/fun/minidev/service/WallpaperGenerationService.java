package org.philipp.fun.minidev.service;

import org.philipp.fun.minidev.pipeline.PipelineContext;
import org.philipp.fun.minidev.pipeline.wallpaper.PipelineProgressListener;
import org.philipp.fun.minidev.pipeline.wallpaper.PipelineProgressSseService;
import org.philipp.fun.minidev.pipeline.wallpaper.WallpaperPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class WallpaperGenerationService {

    private static final Logger log = LoggerFactory.getLogger(WallpaperGenerationService.class);

    private final WallpaperPipeline wallpaperPipeline;
    private final PipelineProgressSseService pipelineProgressSseService;
    private final PipelineExecutionQueueService pipelineExecutionQueueService;

    public WallpaperGenerationService(
            WallpaperPipeline wallpaperPipeline,
            PipelineProgressSseService pipelineProgressSseService,
            PipelineExecutionQueueService pipelineExecutionQueueService
    ) {
        this.wallpaperPipeline = wallpaperPipeline;
        this.pipelineProgressSseService = pipelineProgressSseService;
        this.pipelineExecutionQueueService = pipelineExecutionQueueService;
    }

    @Scheduled(cron = "${minidev.wallpaper.cron:0 0 0 * * *}")
    @Transactional
    public void generateDailyWallpaper() {
        log.info("Generating daily wallpaper.");
        enqueueWallpaperGeneration();
    }

    public void generateNewWallpaper() {
        CompletableFuture<Boolean> generationFuture = enqueueWallpaperGeneration();
        try {
            boolean success = generationFuture.get();
            if (!success) {
                log.warn("Wallpaper generation finished without success");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Wallpaper generation was interrupted while waiting for queue execution", e);
        } catch (ExecutionException e) {
            log.error("Wallpaper generation failed in queue execution", e.getCause());
        }
    }

    public CompletableFuture<Boolean> enqueueWallpaperGeneration() {
        return pipelineExecutionQueueService.submit(this::generateNewWallpaperInNewTransaction);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean generateNewWallpaperInNewTransaction() {
        PipelineContext context = new PipelineContext();
        context.setPipeline(wallpaperPipeline);

        String runId = UUID.randomUUID().toString();
        PipelineProgressListener listener = new PipelineProgressListener(
                runId,
                wallpaperPipeline.getName(),
                wallpaperPipeline,
                pipelineProgressSseService
        );

        wallpaperPipeline.setListeners(List.of(listener));
        pipelineProgressSseService.startRun(runId, wallpaperPipeline.getName());
        listener.markRootStarted();

        try {
            boolean success = wallpaperPipeline.execute(context);
            listener.markRootFinished(success);
            pipelineProgressSseService.finishRun(runId, wallpaperPipeline.getName(), success);

            if (!success) {
                log.error("Failed to generate wallpaper: Pipeline execution returned false");
            }
            return success;
        } catch (Exception e) {
            listener.markRootFinished(false);
            pipelineProgressSseService.finishRun(runId, wallpaperPipeline.getName(), false);
            log.error("Error during wallpaper generation: {}", e.getMessage(), e);
            return false;
        } finally {
            wallpaperPipeline.setListeners(List.of());
        }
    }
}
