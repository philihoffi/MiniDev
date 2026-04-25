package org.philipp.fun.minidev.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PipelineExecutionQueueService Tests")
class PipelineExecutionQueueServiceTest {

    @Test
    @DisplayName("Executes queued pipeline tasks in FIFO order")
    void executesTasksInFifoOrder() throws ExecutionException, InterruptedException {
        PipelineExecutionQueueService queueService = new PipelineExecutionQueueService();
        try {
            List<Integer> executionOrder = Collections.synchronizedList(new ArrayList<>());
            List<CompletableFuture<Integer>> futures = new ArrayList<>();

            for (int i = 0; i < 5; i++) {
                final int taskId = i;
                futures.add(queueService.submit(() -> {
                    executionOrder.add(taskId);
                    Thread.sleep(25L);
                    return taskId;
                }));
            }

            for (CompletableFuture<Integer> future : futures) {
                future.get();
            }

            assertThat(executionOrder).containsExactly(0, 1, 2, 3, 4);
        } finally {
            queueService.shutdown();
        }
    }

    @Test
    @DisplayName("Never runs more than one pipeline task in parallel")
    void neverRunsMoreThanOneTaskInParallel() throws ExecutionException, InterruptedException {
        PipelineExecutionQueueService queueService = new PipelineExecutionQueueService();
        try {
            AtomicInteger activeTasks = new AtomicInteger(0);
            AtomicInteger maxActiveTasks = new AtomicInteger(0);
            List<CompletableFuture<Boolean>> futures = new ArrayList<>();

            for (int i = 0; i < 8; i++) {
                futures.add(queueService.submit(() -> {
                    int nowActive = activeTasks.incrementAndGet();
                    maxActiveTasks.updateAndGet(current -> Math.max(current, nowActive));

                    try {
                        Thread.sleep(40L);
                    } finally {
                        activeTasks.decrementAndGet();
                    }
                    return true;
                }));
            }

            for (CompletableFuture<Boolean> future : futures) {
                future.get();
            }

            assertThat(maxActiveTasks.get()).isEqualTo(1);
        } finally {
            queueService.shutdown();
        }
    }
}

