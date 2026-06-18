package com.example.Parallel;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ThreadPoolTest {

    @Autowired
    private ThreadPoolTaskExecutor orderExecutor;

    @Test
    void compareSpringPoolVsRawExecutor() throws Exception {

        int tasks = 800;
        long durationMs = 3000;
        int cores = Runtime.getRuntime().availableProcessors();
        int rawPoolSize = cores * (1 + 10);
        System.out.println("========================================");
        System.out.println("THREAD POOL TEST");
        System.out.println("========================================");

        long springTime = runSpringPoolTest(tasks, durationMs);

        System.out.println("========================================");
        System.out.println("NO THREAD POOL TEST");
        System.out.println("========================================");

        long rawTime = runRawExecutorTest(tasks, durationMs, rawPoolSize);

        System.out.println("========================================");
        System.out.println("FINAL COMPARISON");
        System.out.println("========================================");

        System.out.println("Thread Pool Time : " + springTime + " ms");
        System.out.println("No Thread Pool Time: " + rawTime + " ms");

    }

    private long runSpringPoolTest(
            int tasks,
            long durationMs
    ) throws Exception {

        AtomicInteger successCount  = new AtomicInteger();
        AtomicInteger rejectedCount = new AtomicInteger();
        AtomicInteger failedCount   = new AtomicInteger();

        List<CompletableFuture<String>> futures =
                new ArrayList<>();

        CountDownLatch startLatch =
                new CountDownLatch(1);

        long startTime =
                System.currentTimeMillis();

        Map<String, Object> before =
                getSpringPoolStats();

        for (int i = 1; i <= tasks; i++) {

            final int taskId = i;

            try {

                CompletableFuture<String> future =
                        CompletableFuture.supplyAsync(() -> {

                            try {

                                startLatch.await();

                                Thread.sleep(durationMs);

                                successCount.incrementAndGet();

                                return "Task-" + taskId
                                        + " completed";

                            } catch (InterruptedException e) {

                                Thread.currentThread().interrupt();

                                failedCount.incrementAndGet();

                                return "Task-" + taskId
                                        + " interrupted";
                            }

                        }, orderExecutor);

                futures.add(future);

            } catch (RejectedExecutionException e) {

                rejectedCount.incrementAndGet();

                futures.add(
                        CompletableFuture.completedFuture(
                                "Task-" + taskId
                                        + " rejected"
                        )
                );
            }
        }

        startLatch.countDown();

        Map<String, Object> during =
                getSpringPoolStats();

        for (CompletableFuture<String> future : futures) {
            future.get(60, TimeUnit.SECONDS);
        }

        long totalTime =
                System.currentTimeMillis() - startTime;

        Map<String, Object> after =
                getSpringPoolStats();

        System.out.println("Succeeded : "
                + successCount.get());

        System.out.println("Rejected  : "
                + rejectedCount.get());

        System.out.println("Failed    : "
                + failedCount.get());

        System.out.println("Total Time: "
                + totalTime + " ms");

        System.out.println("\n--- BEFORE ---");
        printStats(before);

        System.out.println("\n--- DURING ---");
        printStats(during);

        System.out.println("\n--- AFTER ---");
        printStats(after);

        assertEquals(tasks,
                successCount.get()
                        + rejectedCount.get()
                        + failedCount.get());

        return totalTime;
    }

    private long runRawExecutorTest(
            int tasks,
            long durationMs,
            int poolSize
    ) throws Exception {

        AtomicInteger successCount =
                new AtomicInteger();

        AtomicInteger failedCount =
                new AtomicInteger();

        ExecutorService executor =
                Executors.newFixedThreadPool(poolSize);

        List<Future<String>> futures =
                new ArrayList<>();

        CountDownLatch startLatch =
                new CountDownLatch(1);

        long startTime =
                System.currentTimeMillis();

        for (int i = 1; i <= tasks; i++) {

            final int taskId = i;

            Future<String> future =
                    executor.submit(() -> {

                        try {

                            startLatch.await();

                            Thread.sleep(durationMs);

                            successCount.incrementAndGet();

                            return "Task-" + taskId
                                    + " completed";

                        } catch (InterruptedException e) {

                            Thread.currentThread().interrupt();

                            failedCount.incrementAndGet();

                            return "Task-" + taskId
                                    + " interrupted";
                        }
                    });

            futures.add(future);
        }

        startLatch.countDown();

        for (Future<String> future : futures) {
            future.get(60, TimeUnit.SECONDS);
        }

        executor.shutdown();

        executor.awaitTermination(
                1,
                TimeUnit.MINUTES
        );

        long totalTime =
                System.currentTimeMillis() - startTime;

        System.out.println("Succeeded : "
                + successCount.get());

        System.out.println("Failed    : "
                + failedCount.get());


        System.out.println("Total Time: "
                + totalTime + " ms");

        assertEquals(tasks,
                successCount.get()
                        + failedCount.get());

        return totalTime;
    }

    private Map<String, Object> getSpringPoolStats() {

        Map<String, Object> stats =
                new LinkedHashMap<>();

        stats.put("poolSize",
                orderExecutor.getPoolSize());

        stats.put("activeThreads",
                orderExecutor.getActiveCount());

        stats.put("queueSize",
                orderExecutor
                        .getThreadPoolExecutor()
                        .getQueue()
                        .size());

        stats.put("completedTasks",
                orderExecutor
                        .getThreadPoolExecutor()
                        .getCompletedTaskCount());

        return stats;
    }

    private void printStats(
            Map<String, Object> stats
    ) {

        stats.forEach((k, v) ->
                System.out.println(k + ": " + v));
    }

}