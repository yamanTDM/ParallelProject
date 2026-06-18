package com.example.Parallel;

import com.example.Parallel.service.LoadBalancerService;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

class LoadBalancerTest {

    private static final int TOTAL_REQUESTS = 300;

    private static final int CONCURRENCY = 30;

    private static final int SIMULATED_WORK_MS = 50;

    private static WireMockServer server1;
    private static WireMockServer server2;
    private static WireMockServer server3;

    private LoadBalancerService service;
    private HttpClient httpClient;
    private ExecutorService executor;

    @BeforeAll
    static void startServers() {
        server1 = new WireMockServer(wireMockConfig().port(8081).containerThreads(10));
        server2 = new WireMockServer(wireMockConfig().port(8082).containerThreads(10));
        server3 = new WireMockServer(wireMockConfig().port(8083).containerThreads(10));
        server1.start();
        server2.start();
        server3.start();
    }

    @AfterAll
    static void stopServers() {
        server1.stop();
        server2.stop();
        server3.stop();
    }

    @BeforeEach
    void setUp() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        service = new LoadBalancerService(httpClient);
        executor = Executors.newFixedThreadPool(CONCURRENCY);

        for (WireMockServer server : List.of(server1, server2, server3)) {
            server.stubFor(get(urlEqualTo("/api/products"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withFixedDelay(SIMULATED_WORK_MS)
                            .withBody("[{\"id\":1}]")));
        }
    }

    @AfterEach
    void tearDown() {
        List.of(server1, server2, server3).forEach(WireMockServer::resetAll);
        executor.shutdownNow();
    }

    @Test
    @DisplayName("Concurrent: single server vs load-balanced distribution")
    void shouldShowLoadBalancingImprovementUnderConcurrency() {

        AtomicInteger beforeSuccess = new AtomicInteger(0);
        AtomicInteger beforeErrors  = new AtomicInteger(0);

        long beforeStart = System.currentTimeMillis();

        List<CompletableFuture<Void>> beforeFutures = new ArrayList<>();
        for (int i = 0; i < TOTAL_REQUESTS; i++) {
            beforeFutures.add(CompletableFuture.runAsync(() -> {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8081/api/products"))
                            .timeout(Duration.ofSeconds(5))
                            .GET()
                            .build();

                    HttpResponse<String> response =
                            httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) beforeSuccess.incrementAndGet();
                    else                              beforeErrors.incrementAndGet();

                } catch (Exception e) {
                    beforeErrors.incrementAndGet();
                }
            }, executor));
        }

        CompletableFuture.allOf(beforeFutures.toArray(new CompletableFuture[0])).join();

        long beforeElapsed = System.currentTimeMillis() - beforeStart;
        int beforeHits1 = server1.getAllServeEvents().size();

        List.of(server1, server2, server3).forEach(WireMockServer::resetRequests);

        AtomicInteger afterSuccess = new AtomicInteger(0);
        AtomicInteger afterErrors  = new AtomicInteger(0);

        long afterStart = System.currentTimeMillis();

        List<CompletableFuture<Void>> afterFutures = new ArrayList<>();
        for (int i = 0; i < TOTAL_REQUESTS; i++) {
            afterFutures.add(CompletableFuture.runAsync(() -> {
                Map<String, Object> result = service.forwardRequest("/api/products");
                if (result.containsKey("error")) afterErrors.incrementAndGet();
                else                              afterSuccess.incrementAndGet();
            }, executor));
        }

        CompletableFuture.allOf(afterFutures.toArray(new CompletableFuture[0])).join();

        long afterElapsed = System.currentTimeMillis() - afterStart;

        int afterHits1 = server1.getAllServeEvents().size();
        int afterHits2 = server2.getAllServeEvents().size();
        int afterHits3 = server3.getAllServeEvents().size();

        printSummarySingle("BEFORE — Single Server", TOTAL_REQUESTS,
                beforeSuccess.get(), beforeErrors.get(), beforeHits1, beforeElapsed);

        printSummary("AFTER — Load Balanced (Round Robin)", TOTAL_REQUESTS,
                afterSuccess.get(), afterErrors.get(),
                afterHits1, afterHits2, afterHits3, afterElapsed);

        printComparison(beforeElapsed, afterElapsed);
    }

    private void printSummary(String title, int total,
                              int success, int errors,
                              int hits1, int hits2, int hits3,
                              long elapsedMs) {

        int serverTotal = hits1 + hits2 + hits3;
        double avgMs = total == 0 ? 0.0 : (double) elapsedMs / total;

        System.out.println();
        System.out.println("=== " + title + " ===");

        System.out.printf("Instance 8081: %d requests (%.1f%%)%n", hits1, pct(hits1, serverTotal));
        System.out.printf("Instance 8092: %d requests (%.1f%%)%n", hits2, pct(hits2, serverTotal));
        System.out.printf("Instance 9083: %d requests (%.1f%%)%n", hits3, pct(hits3, serverTotal));

        System.out.println();
        System.out.println("Total Requests : " + total);
        System.out.println("Successful     : " + success);
        System.out.println("Errors         : " + errors);
        System.out.println("Total Time     : " + elapsedMs + " ms");
        System.out.printf("Avg Request    : %.2f ms%n", avgMs);
    }

    private void printSummarySingle(String title, int total,
                                    int success, int errors,
                                    int hits1,
                                    long elapsedMs) {

        double avgMs = total == 0 ? 0.0 : (double) elapsedMs / total;

        System.out.println();
        System.out.println("=== " + title + " ===");
        System.out.printf("Instance 8081: %d requests (100.0%%)%n", hits1);

        System.out.println();
        System.out.println("Total Requests : " + total);
        System.out.println("Successful     : " + success);
        System.out.println("Errors         : " + errors);
        System.out.println("Total Time     : " + elapsedMs + " ms");
        System.out.printf("Avg Request    : %.2f ms%n", avgMs);
    }

    private void printComparison(long beforeMs, long afterMs) {
        System.out.println();
        System.out.println("=== COMPARISON ===");
        System.out.println("Single Server : " + beforeMs + " ms");
        System.out.println("Load Balanced : " + afterMs + " ms");
    }

    private double pct(int part, int total) {
        return total == 0 ? 0.0 : (part * 100.0) / total;
    }
}