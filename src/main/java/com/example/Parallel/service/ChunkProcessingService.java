package com.example.Parallel.service;

import com.example.Parallel.entity.Order;
import com.example.Parallel.entity.OrderItem;
import com.example.Parallel.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkProcessingService {
    private final OrderRepository orderRepository;


    @Async("orderExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<DailySalesBatchService.ChunkResult> processChunkAsync(
            int pageNumber,
            LocalDateTime start,
            LocalDateTime end,
            int CHUNK_SIZE
    ) {
        String threadName = Thread.currentThread().getName();
        log.info("[BatchJob][{}] Processing chunk {} (offset {})",
                threadName, pageNumber, pageNumber * CHUNK_SIZE);


        Page<Order> page = orderRepository.findPaidOrdersBetween(
                start, end,
                PageRequest.of(pageNumber, CHUNK_SIZE)
        );

        BigDecimal chunkRevenue   = BigDecimal.ZERO;
        int        chunkUnits     = 0;

        for (Order order : page.getContent()) {
            chunkRevenue = chunkRevenue.add(order.getTotal());

            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    chunkUnits += item.getQuantity();
                }
            }
        }

        log.info("[BatchJob][{}] Chunk {} done → {} orders, revenue={}",
                threadName, pageNumber, page.getNumberOfElements(), chunkRevenue);

        return CompletableFuture.completedFuture(
                new DailySalesBatchService.ChunkResult(page.getNumberOfElements(), chunkRevenue, chunkUnits)
        );
    }


    @Async("orderExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<DailySalesBatchService.ChunkResult> processChunkTest(
            int pageNumber,
            LocalDateTime start,
            LocalDateTime end,
            int CHUNK_SIZE
    ) {
        String threadName = Thread.currentThread().getName();
        log.info("[BatchJob][{}] Processing chunk {} (offset {})",
                threadName, pageNumber, pageNumber * CHUNK_SIZE);


        Page<Order> page = orderRepository.findPaidOrdersBetween(
                start, end,
                PageRequest.of(pageNumber, CHUNK_SIZE)
        );

        BigDecimal chunkRevenue   = BigDecimal.ZERO;
        int        chunkUnits     = 0;

        for (Order order : page.getContent()) {
            chunkRevenue = chunkRevenue.add(order.getTotal());

            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    chunkUnits += item.getQuantity();
                }
            }
        }

        log.info("[BatchJob][{}] Chunk {} done → {} orders, revenue={}",
                threadName, pageNumber, page.getNumberOfElements(), chunkRevenue);

        return CompletableFuture.completedFuture(
                new DailySalesBatchService.ChunkResult(page.getNumberOfElements(), chunkRevenue, chunkUnits)
        );
    }


}
