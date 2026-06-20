package org.philipp.fun.minidev.wallpaper.service;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

/**
 * Service that serializes pipeline execution through a single-threaded queue.
 */
@Service
public class PipelineExecutionQueueService {

    /** Logger for this class. */
    private static final Logger LOG = LoggerFactory.getLogger(PipelineExecutionQueueService.class);

    /** Counter for naming worker threads. */
    private static final AtomicInteger WORKER_COUNTER = new AtomicInteger(1);

    /** Single-threaded executor that serializes pipeline tasks. */
    private final ThreadPoolExecutor pipelineExecutor = new ThreadPoolExecutor(
            1,
            1,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            runnable -> {
                Thread thread = new Thread(runnable);
                thread.setName("pipeline-queue-worker-" + WORKER_COUNTER.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
    );

    /**
     * Submits a pipeline task for serialized execution.
     *
     * @param <T>          the result type
     * @param pipelineTask the task to execute
     * @return a CompletableFuture that completes with the task result
     */
    public <T> CompletableFuture<T> submit(Callable<T> pipelineTask) {
        Objects.requireNonNull(pipelineTask, "pipelineTask must not be null");

        CompletableFuture<T> resultFuture = new CompletableFuture<>();
        pipelineExecutor.submit(() -> {
            try {
                resultFuture.complete(pipelineTask.call());
            } catch (Exception e) {
                resultFuture.completeExceptionally(e);
            }
        });

        LOG.debug("Pipeline enqueued. queuedPipelines={}", pipelineExecutor.getQueue().size());
        return resultFuture;
    }

    /**
     * Gracefully shuts down the pipeline executor.
     */
    @PreDestroy
    public void shutdown() {
        pipelineExecutor.shutdown();
        try {
            if (!pipelineExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                pipelineExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pipelineExecutor.shutdownNow();
        }
    }
}
