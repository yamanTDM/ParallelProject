package com.example.Parallel.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class LoadBalancerService {

    private static final Logger log = LoggerFactory.getLogger(LoadBalancerService.class);

    private static final Duration COOLDOWN = Duration.ofSeconds(15);

    private final List<String> instances = List.of(
            "http://localhost:8081",
            "http://localhost:8082",
            "http://localhost:8083"
    );

    private final Map<String, Instant> downSince = new ConcurrentHashMap<>();

    private final AtomicInteger counter = new AtomicInteger(0);
    private final HttpClient httpClient;



    public Map<String, Object> forwardRequest(String path) {

        List<String> available = instances.stream()
                .filter(url -> !isDown(url))
                .toList();

        if (available.isEmpty()) {
            log.error("All {} instances are unavailable for path: {}", instances.size(), path);
            return Map.of(
                    "strategy", "ROUND_ROBIN",
                    "error",    "All instances unavailable"
            );
        }

        int idx = counter.getAndIncrement() & Integer.MAX_VALUE;
        String instanceUrl = available.get(idx % available.size());
        String targetUrl   = instanceUrl + path;

        log.info("Forwarding request → {}", targetUrl);

        long start = System.currentTimeMillis();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            long elapsed = System.currentTimeMillis() - start;
            markUp(instanceUrl);

            return Map.of(
                    "instance",  instanceUrl,
                    "strategy",  "ROUND_ROBIN",
                    "status",    response.statusCode(),
                    "body",      response.body(),
                    "elapsedMs", elapsed
            );

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("Instance {} failed ({}ms): {} — marking down", instanceUrl, elapsed, e.getMessage());
            markDown(instanceUrl);


            return forwardRequest(path);
        }
    }

    private boolean isDown(String url) {
        Instant since = downSince.get(url);
        if (since == null) return false;

        if (Instant.now().isAfter(since.plus(COOLDOWN))) {
            downSince.remove(url);
            log.info("Cooldown expired for {} — will probe on next request", url);
            return false;
        }
        return true;
    }

    private void markDown(String url) {
        downSince.put(url, Instant.now());
    }

    private void markUp(String url) {
        downSince.remove(url);
    }



    public List<Map<String, Object>> getInstancesStatus() {
        List<Map<String, Object>> status = new ArrayList<>();

        for (String url : instances) {
            boolean healthy = checkHealth(url);

            if (healthy) markUp(url); else markDown(url);

            Instant since = downSince.get(url);
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("url",       url);
            info.put("healthy",   healthy);
            info.put("downSince", since != null ? since.toString() : null);
            status.add(info);
        }
        return status;
    }

    public boolean checkHealth(String baseUrl) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/health"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();

            HttpResponse<String> res =
                    httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            return res.statusCode() == 200;

        } catch (Exception e) {
            return false;
        }
    }
}