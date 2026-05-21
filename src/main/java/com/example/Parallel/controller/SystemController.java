package com.example.Parallel.controller;



import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final ThreadPoolTaskExecutor orderExecutor;

    @GetMapping("/pool-stats")
    public ResponseEntity<Map<String, Object>> poolStats() {
        var threadPool = orderExecutor.getThreadPoolExecutor();

        return ResponseEntity.ok(Map.of(
                "activeThreads",    orderExecutor.getActiveCount(),
                "poolSize",         orderExecutor.getPoolSize(),
                "queueSize",        threadPool.getQueue().size(),
                "completedTasks",   threadPool.getCompletedTaskCount()
        ));
    }
}

