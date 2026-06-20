package org.philipp.fun.minidev.service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.philipp.fun.minidev.llm.LlmClient;
import org.philipp.fun.minidev.pipeline.core.ContextKeys;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;
import org.philipp.fun.minidev.pipeline.registry.PipelineRegistry;
import org.philipp.fun.minidev.pipeline.wallpaper.PipelineProgressListener;
import org.philipp.fun.minidev.pipeline.wallpaper.PipelineProgressSseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for orchestrating wallpaper generation via the pipeline system.
 */
@Service
public class WallpaperGenerationService {

    /** Logger. */
    private static final Logger LOG =
            LoggerFactory.getLogger(WallpaperGenerationService.class);

    /** The wallpaper generation pipeline. */
    private final PipelineElement wallpaperPipeline;

    /** SSE service for pipeline progress. */
    private final PipelineProgressSseService pipelineProgressSseService;

    /** Queue service for sequential pipeline execution. */
    private final PipelineExecutionQueueService pipelineExecutionQueueService;

    /** LLM client for AI model interaction. */
    private final LlmClient llmClient;

    /**
     * Constructs a new WallpaperGenerationService.
     *
     * @param pipelineRegistry             the pipeline registry
     * @param pipelineProgressSseService   the SSE progress service
     * @param pipelineExecutionQueueService the execution queue service
     * @param llmClient                    the LLM client
     */
    public WallpaperGenerationService(
            PipelineRegistry pipelineRegistry,
            PipelineProgressSseService pipelineProgressSseService,
            PipelineExecutionQueueService pipelineExecutionQueueService,
            LlmClient llmClient
    ) {
        this.pipelineProgressSseService = pipelineProgressSseService;
        this.pipelineExecutionQueueService = pipelineExecutionQueueService;
        this.llmClient = llmClient;
        this.wallpaperPipeline = resolvePipeline(pipelineRegistry);
    }

    /**
     * Resolves the wallpaper pipeline from the registry.
     *
     * @param pipelineRegistry the pipeline registry
     * @return the wallpaper pipeline
     */
    private PipelineElement resolvePipeline(PipelineRegistry pipelineRegistry) {
        if (pipelineRegistry.containsPipeline("wallpaper")) {
            LOG.info(
                    "Using YAML-defined 'wallpaper' pipeline"
                    + " from PipelineRegistry");
            return pipelineRegistry.getPipeline("wallpaper");
        }
        throw new IllegalStateException(
                "No YAML pipeline 'wallpaper' found in PipelineRegistry");
    }

    /**
     * Generates a daily wallpaper on a cron schedule.
     */
    @Scheduled(cron = "${minidev.wallpaper.cron:0 0 0 * * *}")
    @Transactional
    public void generateDailyWallpaper() {
        LOG.info("Generating daily wallpaper.");
        enqueueWallpaperGeneration();
    }

    /**
     * Generates a new wallpaper synchronously, waiting for completion.
     */
    public void generateNewWallpaper() {
        CompletableFuture<Boolean> generationFuture =
                enqueueWallpaperGeneration();
        try {
            boolean success = generationFuture.get();
            if (!success) {
                LOG.warn(
                        "Wallpaper generation finished without success");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn(
                    "Wallpaper generation was interrupted"
                    + " while waiting for queue execution",
                    e);
        } catch (ExecutionException e) {
            LOG.error(
                    "Wallpaper generation failed in queue execution",
                    e.getCause());
        }
    }

    /**
     * Enqueues a wallpaper generation task.
     *
     * @return a future that completes with the generation result
     */
    public CompletableFuture<Boolean> enqueueWallpaperGeneration() {
        return pipelineExecutionQueueService.submit(
                this::generateNewWallpaperInNewTransaction);
    }

    /**
     * Executes wallpaper generation in a new transaction.
     *
     * @return true if generation succeeded
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean generateNewWallpaperInNewTransaction() {
        PipelineContext context = new PipelineContext();
        context.setPipeline(wallpaperPipeline);
        context.putValue(ContextKeys.System.LLM_CLIENT, llmClient);

        String runId = UUID.randomUUID().toString();
        PipelineProgressListener listener = new PipelineProgressListener(
                runId,
                wallpaperPipeline.getName(),
                wallpaperPipeline,
                pipelineProgressSseService
        );

        wallpaperPipeline.setListeners(List.of(listener));
        pipelineProgressSseService.startRun(
                runId, wallpaperPipeline.getName());
        listener.markRootStarted();

        try {
            boolean success = wallpaperPipeline.execute(context);
            listener.markRootFinished(success);
            pipelineProgressSseService.finishRun(
                    runId, wallpaperPipeline.getName(), success);

            if (!success) {
                LOG.error(
                        "Failed to generate wallpaper:"
                        + " Pipeline execution returned false");
            }
            return success;
        } catch (Exception e) {
            listener.markRootFinished(false);
            pipelineProgressSseService.finishRun(
                    runId, wallpaperPipeline.getName(), false);
            LOG.error(
                    "Error during wallpaper generation: {}",
                    e.getMessage(), e);
            return false;
        } finally {
            wallpaperPipeline.setListeners(List.of());
        }
    }
}