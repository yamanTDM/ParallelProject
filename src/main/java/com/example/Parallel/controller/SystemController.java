package com.example.Parallel.controller;



import com.example.Parallel.monitoring.RequestTimingReport;
import com.example.Parallel.monitoring.RequestTimingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final ThreadPoolTaskExecutor orderExecutor;
    private final RequestTimingService requestTimingService;

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


    @GetMapping("/timings")
    public ResponseEntity<List<RequestTimingReport>> recentTimings(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(requestTimingService.getRecent(limit));
    }


    @GetMapping("/timings/{traceId}")
    public ResponseEntity<RequestTimingReport> timingForRequest(@PathVariable String traceId) {
        RequestTimingReport report = requestTimingService.getReport(traceId);
        if (report == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(report);
    }

    @DeleteMapping("/timings")
    public ResponseEntity<Void> clearTimings() {
        requestTimingService.clearRecent();
        return ResponseEntity.noContent().build();
    }
}

