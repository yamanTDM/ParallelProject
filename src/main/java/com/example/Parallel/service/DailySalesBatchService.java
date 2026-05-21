package com.example.Parallel.service;

import com.example.Parallel.entity.DailySalesReport;
import com.example.Parallel.entity.Order;
import com.example.Parallel.entity.OrderItem;
import com.example.Parallel.repository.DailySalesReportRepository;
import com.example.Parallel.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailySalesBatchService {

    private final OrderRepository orderRepository;
    private final DailySalesReportRepository reportRepository;
    private final ChunkProcessingService  chunkProcessingService;

    private static final int CHUNK_SIZE = 50;



    @Scheduled(cron = "0 0 0 * * *")
    public void runDailyBatchJob() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("[BatchJob] Starting daily sales batch for {}", yesterday);
        processDaySales(yesterday);
    }



    public DailySalesReport processDaySales(LocalDate date) {
        String dateStr = date.toString();

        if (reportRepository.findByReportDate(dateStr).isPresent()) {
            log.warn("[BatchJob] Report for {} already exists, skipping.", date);
            return reportRepository.findByReportDate(dateStr).get();
        }
        long startTime = System.currentTimeMillis();

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd   = date.atTime(23, 59, 59);

        long totalOrderCount = orderRepository.countPaidOrdersBetween(dayStart, dayEnd);
        log.info("[BatchJob] Found {} paid orders for {}. Splitting into chunks of {}.",
                totalOrderCount, date, CHUNK_SIZE);

        if (totalOrderCount == 0) {
            return saveEmptyReport(dateStr, startTime);
        }

        int totalChunks = (int) Math.ceil((double) totalOrderCount / CHUNK_SIZE);


        List<CompletableFuture<ChunkResult>> futures = new ArrayList<>();

        for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
            final int pageNumber = chunkIndex;
            log.info("[BatchJob] Submitting chunk {}/{}", chunkIndex + 1, totalChunks);

            CompletableFuture<ChunkResult> future = chunkProcessingService.processChunkAsync(pageNumber, dayStart, dayEnd,CHUNK_SIZE);
            futures.add(future);
        }


        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        BigDecimal totalRevenue    = BigDecimal.ZERO;
        int        totalUnitsSold  = 0;
        int        totalOrders     = 0;

        for (CompletableFuture<ChunkResult> future : futures) {
            ChunkResult result = future.join(); // already done, won't block
            totalRevenue   = totalRevenue.add(result.revenue);
            totalUnitsSold += result.unitsSold;
            totalOrders    += result.orderCount;
        }

        long elapsed = System.currentTimeMillis() - startTime;

        DailySalesReport report = new DailySalesReport();
        report.setReportDate(dateStr);
        report.setTotalOrders(totalOrders);
        report.setTotalRevenue(totalRevenue);
        report.setTotalUnitsSold(totalUnitsSold);
        report.setChunksProcessed(totalChunks);
        report.setProcessingTimeMs(elapsed);
        report.setGeneratedAt(LocalDateTime.now());

        DailySalesReport saved = reportRepository.save(report);

        log.info("[BatchJob] ✓ Finished. Orders={}, Revenue={}, Units={}, Chunks={}, Time={}ms",
                totalOrders, totalRevenue, totalUnitsSold, totalChunks, elapsed);

        return saved;
    }





    private DailySalesReport saveEmptyReport(String dateStr, long startTime) {
        DailySalesReport report = new DailySalesReport();
        report.setReportDate(dateStr);
        report.setTotalOrders(0);
        report.setTotalRevenue(BigDecimal.ZERO);
        report.setTotalUnitsSold(0);
        report.setChunksProcessed(0);
        report.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        report.setGeneratedAt(LocalDateTime.now());
        return reportRepository.save(report);
    }



    record ChunkResult(int orderCount, BigDecimal revenue, int unitsSold) {}


    public DailySalesReport processDaySalesTest(LocalDate date, int chunkSize) {
        String dateStr = date.toString();


        long startTime = System.currentTimeMillis();

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd   = date.atTime(23, 59, 59);

        long totalOrderCount = orderRepository.countPaidOrdersBetween(dayStart, dayEnd);
        log.info("[BatchJob] Found {} paid orders for {}. Splitting into chunks of {}.",
                totalOrderCount, date, chunkSize);

        if (totalOrderCount == 0) {
            return saveEmptyReport(dateStr, startTime);
        }

        int totalChunks = (int) Math.ceil((double) totalOrderCount / chunkSize);


        List<CompletableFuture<ChunkResult>> futures = new ArrayList<>();

        for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
            final int pageNumber = chunkIndex;
            log.info("[BatchJob] Submitting chunk {}/{}", chunkIndex + 1, totalChunks);

            CompletableFuture<ChunkResult> future = chunkProcessingService.processChunkTest(pageNumber, dayStart, dayEnd,chunkSize);
            futures.add(future);
        }


        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        BigDecimal totalRevenue    = BigDecimal.ZERO;
        int        totalUnitsSold  = 0;
        int        totalOrders     = 0;

        for (CompletableFuture<ChunkResult> future : futures) {
            ChunkResult result = future.join(); // already done, won't block
            totalRevenue   = totalRevenue.add(result.revenue);
            totalUnitsSold += result.unitsSold;
            totalOrders    += result.orderCount;
        }

        long elapsed = System.currentTimeMillis() - startTime;

        DailySalesReport report = new DailySalesReport();
        report.setReportDate(dateStr);
        report.setTotalOrders(totalOrders);
        report.setTotalRevenue(totalRevenue);
        report.setTotalUnitsSold(totalUnitsSold);
        report.setChunksProcessed(totalChunks);
        report.setProcessingTimeMs(elapsed);
        report.setGeneratedAt(LocalDateTime.now());

        DailySalesReport saved = reportRepository.save(report);

        log.info("[BatchJob] ✓ Finished. Orders={}, Revenue={}, Units={}, Chunks={}, Time={}ms",
                totalOrders, totalRevenue, totalUnitsSold, totalChunks, elapsed);

        return saved;
    }


}