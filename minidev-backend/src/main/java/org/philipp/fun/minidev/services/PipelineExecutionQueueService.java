package org.philipp.fun.minidev.services;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PipelineExecutionQueueService {

    private static final Logger log = LoggerFactory.getLogger(PipelineExecutionQueueService.class);
    private static final AtomicInteger WORKER_COUNTER = new AtomicInteger(1);

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

        log.debug("Pipeline enqueued. queuedPipelines={}", pipelineExecutor.getQueue().size());
        return resultFuture;
    }

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
